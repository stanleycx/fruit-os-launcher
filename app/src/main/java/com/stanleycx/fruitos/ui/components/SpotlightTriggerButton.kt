package com.stanleycx.fruitos.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stanleycx.fruitos.ui.components.GlassLevel
import com.stanleycx.fruitos.ui.components.GlassTint
import com.stanleycx.fruitos.ui.components.glass
import com.stanleycx.fruitos.ui.components.LoupeLevel
import com.stanleycx.fruitos.ui.components.GlossLevel
import dev.chrisbanes.haze.HazeState

@Composable
fun SpotlightTriggerButton(
    onClick: () -> Unit,
    hazeState: HazeState,
    glassLevel: GlassLevel = GlassLevel.Regular,
    glassTint: GlassTint = GlassTint.None,
    customTintColor: Color? = null,
    loupeLevel: LoupeLevel = LoupeLevel.None,
    glossLevel: GlossLevel = GlossLevel.None,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(32.dp)
            .clip(CircleShape)
            .glass(
                hazeState = hazeState,
                level = glassLevel,
                glassTint = glassTint,
                customTintColor = customTintColor,
                shape = CircleShape,
                loupeLevel = loupeLevel, glossLevel = glossLevel
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Recherche",
                tint = Color.White,
                modifier = Modifier
                    .size(17.dp)
                    .offset(y = 1.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "Recherche",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}