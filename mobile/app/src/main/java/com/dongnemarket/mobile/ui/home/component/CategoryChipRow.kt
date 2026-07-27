package com.dongnemarket.mobile.ui.home.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dongnemarket.mobile.domain.model.Category
import com.dongnemarket.mobile.ui.theme.MarketOnTheme

/**
 * 카테고리 필터 칩 줄.
 *
 * 맨 앞의 "전체" 칩은 서버에 없는 **앱 전용 칩**이다(`selectedCategoryId == null` 상태를 고르는 수단).
 *
 * ⚠️ **아이콘 없는 텍스트 칩이다.** `CategoryResponse` 에는 `id`, `name` 두 필드밖에 없어서
 * 아이콘·정렬순서를 서버에서 받을 수 없다(계약 §8-8). 이름→벡터리소스 매핑 테이블을 앱에
 * 하드코딩하는 방법도 있지만, 서버가 카테고리를 추가하면 그 칩만 아이콘이 비거나 기본 아이콘으로
 * 떨어져 줄이 들쭉날쭉해진다. Phase 1 은 텍스트만으로 통일했다.
 *
 * 칩 순서는 서버가 주는 `id ASC`(시드 등록순) 그대로다 — 클라이언트에서 재정렬하지 않는다.
 *
 * @param selectedCategoryId null 이면 "전체" 칩이 선택된 상태.
 * @param onSelect 선택된 카테고리 id. "전체" 를 누르면 null 이 온다.
 */
@Composable
fun CategoryChipRow(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onSelect: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        // 좌우 여백을 padding 이 아니라 contentPadding 으로 주는 이유:
        // 스크롤되는 칩이 화면 가장자리에서 잘리지 않고 여백 안에서 자연스럽게 사라진다.
        contentPadding = PaddingValues(horizontal = 2.dp),
    ) {
        item {
            CategoryChip(
                label = "전체",
                selected = selectedCategoryId == null,
                onClick = { onSelect(null) },
            )
        }
        // 카테고리 조회가 실패하면 이 부분이 비고 "전체" 칩만 남는다.
        // 화면 전체를 에러로 덮지 않는 이유: 칩은 부가 필터이고 상품 목록은 그대로 볼 수 있다.
        items(items = categories, key = { it.id }) { category ->
            CategoryChip(
                label = category.name,
                selected = selectedCategoryId == category.id,
                onClick = { onSelect(category.id) },
            )
        }
    }
}

/** 칩 하나. 선택 색은 테마 토큰(primaryContainer)만 쓴다 — hex 하드코딩 금지. */
@Composable
private fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        shape = MaterialTheme.shapes.small,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

@Preview(name = "카테고리 칩 (전체 선택)", showBackground = true)
@Composable
private fun CategoryChipRowPreview() {
    MarketOnTheme {
        CategoryChipRow(
            categories = previewCategories,
            selectedCategoryId = null,
            onSelect = {},
        )
    }
}

@Preview(name = "카테고리 칩 (의류 선택)", showBackground = true)
@Composable
private fun CategoryChipRowSelectedPreview() {
    MarketOnTheme {
        CategoryChipRow(
            categories = previewCategories,
            selectedCategoryId = 4L,
            onSelect = {},
        )
    }
}

/** 미리보기용 시드 카테고리(실제 서버 시드 8종과 같은 이름·순서). */
private val previewCategories = listOf(
    Category(id = 1L, name = "디지털기기"),
    Category(id = 2L, name = "생활가전"),
    Category(id = 3L, name = "가구/인테리어"),
    Category(id = 4L, name = "의류"),
    Category(id = 5L, name = "도서"),
    Category(id = 6L, name = "스포츠/레저"),
    Category(id = 7L, name = "반려동물용품"),
    Category(id = 8L, name = "기타"),
)
