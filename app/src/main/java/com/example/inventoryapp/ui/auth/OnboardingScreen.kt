package com.example.inventoryapp.ui.auth

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventoryapp.R
import com.example.inventoryapp.ui.theme.InventoryAppTheme
import com.example.inventoryapp.ui.theme.NawanoLightOutline
import com.example.inventoryapp.ui.theme.NawanoLightSurface
import com.example.inventoryapp.ui.theme.NawanoNeutral50
import com.example.inventoryapp.ui.theme.NawanoPrimary100
import com.example.inventoryapp.ui.theme.NawanoPrimary200
import com.example.inventoryapp.ui.theme.NawanoTextPrimaryLight
import com.example.inventoryapp.ui.theme.NawanoTextSecondaryLight
import kotlinx.coroutines.launch

private data class IntroPage(
    @param:DrawableRes val image: Int,
    @param:DrawableRes val iconRes: Int,
    val title: String,
    val description: String
)

private val introPages = listOf(
    IntroPage(
        image = R.drawable.onboarding_1,
        iconRes = R.drawable.ic_bus_intro,
        title = "Easy way to manage your inventory business",
        description = "Simplify your inventory management and boost productivity with our intuitive app."
    ),
    IntroPage(
        image = R.drawable.onboarding_2,
        iconRes = R.drawable.ic_pos_cart,
        title = "Effortlessly track inventory details with scanning",
        description = "Streamline inventory management by scanning items for quick, accurate tracking."
    ),
    IntroPage(
        image = R.drawable.onboarding_3,
        iconRes = R.drawable.ic_inventory_box,
        title = "Empowering success through report analysis",
        description = "Optimize inventory management by analyzing detailed reports."
    )
)

/**
 * First-launch intro: 3 swipeable pages with Skip / Next, last page shows Get Started.
 * Always uses the light Nawano look from the design (it is shown before any settings exist).
 *
 * @param onFinished called on Skip or Get Started.
 */
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { introPages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == introPages.lastIndex

    // System back goes to the previous intro page instead of closing the app.
    BackHandler(enabled = pagerState.currentPage > 0) {
        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NawanoNeutral50)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top: swipeable phone mockups
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) { page ->
                Image(
                    painter = painterResource(introPages[page].image),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Bottom: white rounded sheet with text, dots and buttons
            Surface(
                color = NawanoLightSurface,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 22.dp)
                ) {
                    val page = introPages[pagerState.currentPage]

                    // Intro Brand Feature Icon (Bus icon for transit/intro)
                    Surface(
                        shape = CircleShape,
                        color = NawanoPrimary100.copy(alpha = 0.22f),
                        border = BorderStroke(1.dp, NawanoPrimary200.copy(alpha = 0.35f)),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(page.iconRes),
                                contentDescription = "Intro Feature Icon",
                                tint = NawanoPrimary200,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = page.title,
                        color = NawanoTextPrimaryLight,
                        fontSize = 22.sp,
                        lineHeight = 30.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = page.description,
                        color = NawanoTextSecondaryLight,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Spacer(Modifier.height(20.dp))
                    PageDots(count = introPages.size, current = pagerState.currentPage)
                    Spacer(Modifier.height(24.dp))

                    if (isLastPage) {
                        PrimaryButton(
                            text = "Get Started",
                            onClick = onFinished,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = onFinished,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, NawanoLightOutline),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = NawanoTextPrimaryLight
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                            ) {
                                Text("Skip", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            }
                            PrimaryButton(
                                text = "Next",
                                onClick = {
                                    scope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = NawanoPrimary200,
            contentColor = NawanoLightSurface
        ),
        modifier = modifier.height(52.dp)
    ) {
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

/** Active page = wide purple pill, others = small lavender dots (as in the design). */
@Composable
private fun PageDots(count: Int, current: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { index ->
            val selected = index == current
            val width by animateDpAsState(if (selected) 20.dp else 6.dp, label = "dotWidth")
            val color by animateColorAsState(
                if (selected) NawanoPrimary200 else NawanoPrimary100,
                label = "dotColor"
            )
            Box(
                modifier = Modifier
                    .size(height = 6.dp, width = width)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun OnboardingScreenPreview() {
    InventoryAppTheme(darkTheme = false) {
        OnboardingScreen(onFinished = {})
    }
}
