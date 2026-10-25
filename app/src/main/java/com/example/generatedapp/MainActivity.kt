```kotlin
package com.example.generatedapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------- Theme ----------
private val BrandPrimary = Color(0xFF1F2937)
private val BrandAccent = Color(0xFFE11D48)
private val BrandAccentDark = Color(0xFFBE123C)
private val BrandSurface = Color(0xFFF8FAFC)
private val BrandCard = Color(0xFFFFFFFF)
private val BrandTextPrimary = Color(0xFF0F172A)
private val BrandTextSecondary = Color(0xFF64748B)
private val BrandBorder = Color(0xFFE2E8F0)
private val BrandStar = Color(0xFFF59E0B)
private val BrandSuccess = Color(0xFF10B981)

// ---------- Data ----------
private data class ProductImage(
    val gradient: List<Color>,
    val label: String,
    val icon: ImageVector
)

private val productImages = listOf(
    ProductImage(
        gradient = listOf(Color(0xFF1E293B), Color(0xFF334155)),
        label = "Front View",
        icon = Icons.Default.Star
    ),
    ProductImage(
        gradient = listOf(Color(0xFF7C3AED), Color(0xFF4C1D95)),
        label = "Side View",
        icon = Icons.Default.Favorite
    ),
    ProductImage(
        gradient = listOf(Color(0xFF0EA5E9), Color(0xFF0369A1)),
        label = "Back View",
        icon = Icons.Default.ShoppingCart
    ),
    ProductImage(
        gradient = listOf(Color(0xFF10B981), Color(0xFF047857)),
        label = "Detail",
        icon = Icons.Default.Add
    )
)

private val availableSizes = listOf("XS", "S", "M", "L", "XL", "XXL")

// ---------- Root ----------
@Composable
fun AppContent() {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = BrandPrimary,
            onPrimary = Color.White,
            secondary = BrandAccent,
            background = BrandSurface,
            surface = BrandCard
        ),
        typography = MaterialTheme.typography
    ) {
        ProductDetailsScreen()
    }
}

// ---------- Screen ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailsScreen() {
    var selectedSize by remember { mutableStateOf("M") }
    var quantity by remember { mutableIntStateOf(1) }
    var isFavorite by remember { mutableStateOf(false) }
    var isAddedToCart by remember { mutableStateOf(false) }
    var isPurchasing by remember { mutableStateOf(false) }
    var showPurchaseSuccess by remember { mutableStateOf(false) }

    val basePrice = 129.00
    val discount = 0.20
    val discountedPrice = basePrice * (1 - discount)
    val total = discountedPrice * quantity

    Scaffold(
        containerColor = BrandSurface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Product Details",
                        style = MaterialTheme.typography.titleMedium,
                        color = BrandTextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { /* back */ }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = BrandTextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { isFavorite = !isFavorite }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) BrandAccent else BrandTextSecondary
                        )
                    }
                    IconButton(onClick = { /* share */ }) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Cart",
                            tint = BrandTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrandSurface,
                    titleContentColor = BrandTextPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Image slider
            ImageSlider()

            Spacer(modifier = Modifier.height(16.dp))

            // Title + rating
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Premium Cotton Hoodie",
                    style = MaterialTheme.typography.titleLarge,
                    color = BrandTextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                RatingRow(rating = 4.8, reviewCount = 1243)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Price tag
            PriceTag(
                basePrice = basePrice,
                discountedPrice = discountedPrice,
                discountPercent = (discount * 100).toInt()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Description
            Text(
                text = "Crafted from 100% organic cotton with a soft brushed interior. " +
                        "Features a kangaroo pocket, adjustable drawstring hood, and " +
                        "durable double-stitched seams for everyday comfort.",
                style = MaterialTheme.typography.bodyMedium,
                color = BrandTextSecondary,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Size selector
            Text(
                text = "Select Size",
                style = MaterialTheme.typography.titleSmall,
                color = BrandTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(10.dp))
            SizeSelector(
                sizes = availableSizes,
                selectedSize = selectedSize,
                onSizeSelected = { selectedSize = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Quantity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quantity",
                    style = MaterialTheme.typography.titleSmall,
                    color = BrandTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                QuantitySelector(
                    quantity = quantity,
                    onIncrease = { if (quantity <

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface

class MainActivity : ComponentActivity() {
    override def onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    AppContent()
                }
            }
        }
    }
}
