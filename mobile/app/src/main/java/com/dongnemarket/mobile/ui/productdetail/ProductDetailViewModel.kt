package com.dongnemarket.mobile.ui.productdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dongnemarket.mobile.domain.model.AppError
import com.dongnemarket.mobile.domain.repository.CategoryRepository
import com.dongnemarket.mobile.domain.repository.ChatRepository
import com.dongnemarket.mobile.domain.repository.FavoriteRepository
import com.dongnemarket.mobile.domain.repository.MemberRepository
import com.dongnemarket.mobile.domain.repository.ProductRepository
import com.dongnemarket.mobile.ui.navigation.MarketOnRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 상품 상세 화면의 상태 보관소.
 *
 * Spring 에 빗대면 컨트롤러에 가깝다 — 저장소(Repository)에서 값을 받아 화면이 쓸 모양([ProductDetailUiState])으로
 * 조립하고, 화면에서 올라온 이벤트(찜 탭·채팅하기·재시도)를 처리한다.
 * `android.*` UI 타입(Color·Context·View)은 여기서 절대 참조하지 않는다.
 *
 * ## 이 화면이 저장소 5개를 쓰는 이유
 * 상세 응답 하나로 화면이 완성되지 않는다.
 *  - [productRepository] : 상세 본문
 *  - [favoriteRepository] : **하트 초기값**(상세 응답에 '내가 찜했는지' 가 없다 — 계약 §7-2)
 *  - [categoryRepository] : `categoryId` 숫자를 사람이 읽는 이름으로(상세 응답에 이름이 없다)
 *  - [memberRepository] : 내 `memberId` → '내 상품' 판정(판매자는 자기 상품에 채팅방을 못 만든다)
 *  - [chatRepository] : '채팅하기'
 */
@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val productRepository: ProductRepository,
    private val favoriteRepository: FavoriteRepository,
    private val categoryRepository: CategoryRepository,
    private val memberRepository: MemberRepository,
    private val chatRepository: ChatRepository,
) : ViewModel() {

    /**
     * 경로 변수(`productDetail/{productId}`)를 화면 파라미터가 아니라 여기서 꺼낸다.
     *
     * 왜 [SavedStateHandle] 인가: NavHost 가 넣어 준 인자를 ViewModel 이 직접 읽으면
     * 화면 Composable 은 "id 를 전달받아 넘겨 주는" 배선 코드를 갖지 않아도 되고,
     * 프로세스가 죽었다 복원돼도 같은 id 를 그대로 다시 얻는다.
     */
    private val productId: Long =
        savedStateHandle.get<Long>(MarketOnRoutes.ARG_PRODUCT_ID) ?: 0L

    private val _uiState = MutableStateFlow<ProductDetailUiState>(ProductDetailUiState.Loading)
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    /**
     * 채팅방 생성 성공 = "한 번 일어나고 끝나는 사건" 이라 상태가 아니라 이벤트로 내보낸다.
     *
     * 만약 이걸 `uiState` 의 `roomId` 필드로 두면, 화면 회전 후 상태를 다시 읽는 순간
     * 채팅방으로 또 이동하게 된다. `extraBufferCapacity = 1` 은 화면이 잠깐 구독을 놓쳐도
     * emit 이 코루틴을 멈춰 세우지 않게 하는 여유 칸이다.
     */
    private val _chatRoomEvent = MutableSharedFlow<Long>(extraBufferCapacity = 1)
    val chatRoomEvent: SharedFlow<Long> = _chatRoomEvent.asSharedFlow()

    /** 진행 중인 상세 로드. 재시도 연타로 같은 GET 이 겹쳐 나가는 것을 막는다(그 GET 은 조회수를 올린다). */
    private var loadJob: Job? = null

    /**
     * 찜 토글이 서버 왕복 중인지. 두 가지 용도로 쓴다.
     *  1. 연타 방지 — add/remove 가 교차하면 최종 서버 상태를 예측할 수 없다.
     *  2. 낙관적 갱신 보호 — 아래 [observeFavoriteCache] 가 전역 캐시로 화면을 덮어쓰지 않게 한다.
     */
    private var favoriteInFlight = false

    init {
        observeFavoriteCache()
        load()
    }

    /**
     * 앱 전역 찜 캐시를 구독한다. 다른 화면(홈 카드)에서 찜을 바꿔도 이 화면의 하트가 따라오게 만드는 배선.
     *
     * 토글 진행 중에는 무시하는 이유: 우리는 서버 응답 **전에** 하트를 먼저 켜 두는데(낙관적 갱신),
     * 그 사이 캐시는 아직 옛 값이라 그대로 반영하면 방금 켠 하트가 다시 꺼져 깜빡인다.
     */
    private fun observeFavoriteCache() {
        viewModelScope.launch {
            favoriteRepository.favoriteProductIds.collect { ids ->
                if (favoriteInFlight) return@collect
                _uiState.update { state ->
                    if (state is ProductDetailUiState.Success) {
                        val nowFavorite = productId in ids
                        if (nowFavorite == state.isFavorite) {
                            // 값이 그대로면 개수도 건드리지 않는다.
                            state
                        } else {
                            // 하트가 실제로 뒤집힐 때는 개수도 같은 방향으로 ±1 한다.
                            // 이걸 빼면 '하트는 켜졌는데 찜 개수는 12 그대로' 처럼
                            // 한 화면 안에서 두 값이 서로 모순된 상태가 된다
                            // (홈 카드에서 토글 후 back, 백그라운드 복귀 후 refreshFavorites 도착 등).
                            // 찜 목록 응답에 개수가 없어 서버 값으로 다시 맞출 수단이 없으므로
                            // onFavoriteClick 과 동일한 로컬 ±1 규칙을 쓴다.
                            state.copy(
                                isFavorite = nowFavorite,
                                favoriteCount = (
                                    state.favoriteCount + if (nowFavorite) 1 else -1
                                    ).coerceAtLeast(0),
                            )
                        }
                    } else {
                        state
                    }
                }
            }
        }
    }

    /** 사용자가 에러 화면의 '다시 시도' 를 눌렀을 때. **자동 재시도는 만들지 않는다**(조회수가 오른다). */
    fun retry() {
        load()
    }

    /**
     * 상세 + 부속 정보 로드. `init` 과 사용자의 재시도에서만 호출한다.
     *
     * ⚠ `getProductDetail` 은 서버에서 **`viewCount` 를 +1 하는 쓰기 동작**이다(계약 §7-11).
     * 그래서 화면 재구성마다 부르지 않고 ViewModel 수명당 1회 + 명시적 재시도만 허용하며,
     * [loadJob] 로 중복 실행도 막는다. pull-to-refresh 를 이 화면에 두지 않은 것도 같은 이유다.
     */
    private fun load() {
        if (productId <= 0L) {
            // NavHost 인자가 깨진 경우. 서버를 부르지 않고 바로 실패로 끝낸다.
            _uiState.value = ProductDetailUiState.Error("상품 정보를 불러올 수 없습니다.")
            return
        }
        if (loadJob?.isActive == true) return

        _uiState.value = ProductDetailUiState.Loading
        loadJob = viewModelScope.launch {
            // 부속 3건은 상세와 서로 독립이므로 병렬로 띄운다.
            // 세 개 모두 **실패해도 상세는 보여 준다** → 실패를 삼키고 기본값(하트 꺼짐/이름 없음/내 상품 아님)으로 진행.
            val favoriteRefresh = async { favoriteRepository.refreshFavorites() }
            val myMemberIdAsync = async { memberRepository.getMyProfile().getOrNull()?.memberId }
            val categoriesAsync = async { categoryRepository.getCategories().getOrNull().orEmpty() }

            val detailResult = productRepository.getProductDetail(productId)

            favoriteRefresh.await()
            val myMemberId = myMemberIdAsync.await()
            val categories = categoriesAsync.await()

            detailResult
                .onSuccess { detail ->
                    _uiState.value = ProductDetailUiState.Success(
                        product = detail,
                        // 하트 초기값은 상세 응답이 아니라 방금 갱신한 전역 캐시에서 온다(계약 §7-2).
                        isFavorite = detail.productId in favoriteRepository.favoriteProductIds.value,
                        favoriteCount = detail.favoriteCount,
                        categoryName = categories.firstOrNull { it.id == detail.categoryId }?.name,
                        isMyProduct = myMemberId != null && myMemberId == detail.sellerId,
                    )
                }
                .onFailure { error ->
                    _uiState.value = ProductDetailUiState.Error(error.toDetailMessage())
                }
        }
    }

    /**
     * 하트 탭. **낙관적 갱신**(먼저 화면을 바꾸고 나중에 서버로 확정)을 쓴다.
     *
     * 서버 왕복을 기다렸다 하트를 켜면 100~300ms 동안 버튼이 죽은 것처럼 보인다.
     * 대신 실패했을 때 되돌릴 책임이 생긴다 — 아래 롤백 주석 참고.
     */
    fun onFavoriteClick() {
        val current = _uiState.value as? ProductDetailUiState.Success ?: return
        if (favoriteInFlight) return

        val target = !current.isFavorite
        favoriteInFlight = true
        _uiState.value = current.copy(
            isFavorite = target,
            // 찜 개수를 로컬로 ±1 하는 이유: 등록/취소 응답에 갱신된 개수가 **오지 않고**,
            // 개수를 맞추려고 상세를 재조회하면 그 GET 이 조회수를 +1 해 버린다(계약 §7-11, §3-4).
            // coerceAtLeast(0) 는 200건 하드캡 등으로 캐시와 서버가 어긋났을 때 음수 표시를 막는 안전판이다.
            favoriteCount = (current.favoriteCount + if (target) 1 else -1).coerceAtLeast(0),
        )

        viewModelScope.launch {
            val result = if (target) {
                favoriteRepository.addFavorite(productId)
            } else {
                favoriteRepository.removeFavorite(productId)
            }
            favoriteInFlight = false

            result.onFailure { error ->
                // ── 롤백이 반드시 필요한 이유 ──
                // 위에서 서버 확인 없이 하트를 먼저 바꿨다. 되돌리지 않으면 화면과 서버가 영구히 어긋나
                // "찜했는데 목록에 없다" 가 된다.
                // 그리고 여기 도달하는 실패는 '진짜 반영되지 않은' 실패뿐이다 —
                // 409(이미 찜함)/404 FAVORITE_NOT_FOUND(이미 취소됨)는 Repository 가
                // "원하는 상태 도달" 로 흡수해 success 로 주기 때문에(계약 §7-15) 여기로 오지 않는다.
                // 즉 롤백해도 되는 경우만 롤백한다.
                _uiState.update { state ->
                    if (state is ProductDetailUiState.Success) {
                        state.copy(
                            isFavorite = current.isFavorite,
                            favoriteCount = current.favoriteCount,
                            message = error.toDetailMessage(),
                        )
                    } else {
                        state
                    }
                }
            }
        }
    }

    /**
     * '채팅하기' 탭.
     *
     * `createRoom` 은 **get-or-create 멱등**이라 이미 방이 있으면 같은 `roomId` 가 온다
     * → "방이 있는지 먼저 확인" 같은 분기를 만들지 않는다(계약 §3-6).
     */
    fun onChatClick() {
        val current = _uiState.value as? ProductDetailUiState.Success ?: return
        if (current.isChatCreating) return
        if (current.isMyProduct) {
            // 버튼을 이미 비활성으로 두었지만, 접근성 도구 등으로 눌리는 경로가 있어 방어한다.
            // 서버에 보내면 400 CANNOT_CHAT_WITH_SELF 가 될 요청이므로 애초에 보내지 않는다.
            _uiState.value = current.copy(message = "내 상품에는 채팅을 걸 수 없어요.")
            return
        }

        _uiState.value = current.copy(isChatCreating = true)
        viewModelScope.launch {
            chatRepository.createRoom(productId)
                .onSuccess { roomId ->
                    _uiState.update { state ->
                        if (state is ProductDetailUiState.Success) state.copy(isChatCreating = false) else state
                    }
                    _chatRoomEvent.emit(roomId)
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        if (state is ProductDetailUiState.Success) {
                            state.copy(isChatCreating = false, message = error.toDetailMessage())
                        } else {
                            state
                        }
                    }
                }
        }
    }

    /** 스낵바를 띄운 뒤 호출해 일회성 메시지를 비운다(화면 회전 때 같은 메시지가 다시 뜨지 않게). */
    fun consumeMessage() {
        _uiState.update { state ->
            if (state is ProductDetailUiState.Success && state.message != null) {
                state.copy(message = null)
            } else {
                state
            }
        }
    }

    /**
     * 실패 → 화면에 띄울 한국어 문장.
     *
     * 기본은 `AppError.userMessage`(백엔드가 준 한국어)를 그대로 쓰고, 두 경우만 문구를 부드럽게 바꾼다.
     * 404/403 은 **장애가 아니라 정상 시나리오**(삭제·거래완료·판매자 탈퇴·숨김 상품 — 계약 §7-20)이므로
     * 사용자가 "앱이 고장났다" 고 느끼지 않게 하려는 것이다.
     *
     * `AppError.Api.code` 는 분기에만 쓰고 화면에 노출하지 않는다.
     * 500 을 "서버 장애" 라고 단정하지 않는 것도 의도된 것이다 — 클라이언트 요청 실수도 500 으로 온다(계약 §7-4).
     */
    private fun Throwable.toDetailMessage(): String {
        val appError = this as? AppError ?: return "요청을 처리할 수 없습니다."
        if (appError is AppError.Api) {
            when (appError.status) {
                404 -> return "삭제되었거나 거래가 끝난 상품이에요."
                403 -> return "판매자가 숨긴 상품이에요."
            }
        }
        return appError.userMessage
    }
}
