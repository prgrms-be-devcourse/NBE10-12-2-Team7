package com.dongnemarket.mobile.ui.home.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dongnemarket.mobile.ui.theme.MarketOnTheme

/**
 * 홈 검색바.
 *
 * **입력 초안(draft)을 이 Composable 이 직접 들고 있는 것이 이 컴포넌트의 핵심**이다.
 * 글자가 바뀔 때마다 ViewModel 로 올려 보내면 상태가 갱신되는 만큼 리컴포지션이 돌고,
 * 무엇보다 "타이핑마다 검색 API 호출" 로 이어지기 쉽다. 서버 검색은 페이징 없이 전량을
 * 돌려주는 무거운 조회라서(계약 §2-4) 더더욱 그렇게 하면 안 된다.
 *
 * 그래서 [onSearch] 는 **키보드의 검색 버튼을 눌렀을 때와 X(지우기)를 눌렀을 때만** 호출된다.
 *
 * @param keyword ViewModel 이 들고 있는 **확정된** 검색어. 이 값이 바뀌면 초안도 그 값으로 되돌아간다
 *   (`rememberSaveable(keyword)` 의 key 역할 — 검색 해제 시 입력창도 같이 비워지게 한다).
 * @param onSearch 확정된 검색어를 알린다. 빈 문자열이면 검색 해제(= 기본 목록으로 복귀).
 */
@Composable
fun HomeSearchBar(
    keyword: String,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by rememberSaveable(keyword) { mutableStateOf(keyword) }
    val focusManager = LocalFocusManager.current

    fun commit(value: String) {
        onSearch(value)
        // 검색을 확정했으면 키보드를 내린다 — 목록 결과가 키보드에 가려지지 않게.
        focusManager.clearFocus()
    }

    OutlinedTextField(
        value = draft,
        onValueChange = { draft = it },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        placeholder = { Text("어떤 물건을 찾으세요?") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingIcon = {
            if (draft.isNotEmpty()) {
                IconButton(
                    onClick = {
                        draft = ""
                        // 입력만 지우는 게 아니라 검색 해제까지 확정한다 —
                        // 글자만 사라지고 검색 결과가 그대로 남으면 사용자가 혼란스럽다.
                        commit("")
                    },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "검색어 지우기",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { commit(draft.trim()) }),
    )
}

@Preview(name = "검색바 (빈 상태)", showBackground = true)
@Composable
private fun HomeSearchBarPreview() {
    MarketOnTheme {
        HomeSearchBar(keyword = "", onSearch = {}, modifier = Modifier.padding(16.dp))
    }
}

@Preview(name = "검색바 (검색어 있음)", showBackground = true)
@Composable
private fun HomeSearchBarFilledPreview() {
    MarketOnTheme {
        HomeSearchBar(keyword = "자전거", onSearch = {}, modifier = Modifier.padding(16.dp))
    }
}
