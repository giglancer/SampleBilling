package com.samplebillingapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.samplebillingapp.ui.billing.BillingViewModel
import com.samplebillingapp.ui.billing.state.BillingUiState
import com.samplebillingapp.ui.theme.SampleBillingAppTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val billingViewModel: BillingViewModel by viewModels()
        enableEdgeToEdge()
        setContent {
            val billingUiState = billingViewModel.billingUiState.collectAsState()
            SampleBillingAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LazyColumn(modifier = Modifier.padding(innerPadding)) {
                        item {
                            Button(
                                onClick = {
                                    billingViewModel.retry()
                                }
                            ) {
                                Text(text = "リトライ")
                            }
                        }
                        items(billingUiState.value.products) {
                            Text(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (billingViewModel.isPurchased(it.productId)) {
                                            Modifier.background(Color.Red)
                                        } else {
                                            Modifier.background(Color.Transparent)
                                                .clickable {
                                                billingViewModel.purchaseProduct(
                                                    this@MainActivity,
                                                    it
                                                )
                                            }
                                        }
                                    ),
                                text = it.name,
                                fontSize = 20.sp
                            )
                        }
                    }
                }
            }
        }
    }
}