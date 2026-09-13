package com.porter.feature.documents.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import com.porter.core.common.UiState
import com.porter.core.designsystem.theme.ActionBlue
import com.porter.core.designsystem.theme.BodyDefault
import com.porter.core.designsystem.theme.BodyStrong
import com.porter.core.designsystem.theme.CanvasWhite
import com.porter.core.designsystem.theme.Caption
import com.porter.core.designsystem.theme.FinePrint
import com.porter.core.designsystem.theme.InkMuted48
import com.porter.core.designsystem.theme.InkNearBlack
import com.porter.core.designsystem.theme.Parchment
import com.porter.core.designsystem.theme.StatusError
import com.porter.core.designsystem.theme.StatusSuccess
import com.porter.core.ui.components.ErrorScreen
import com.porter.core.ui.components.LoadingScreen
import com.porter.core.ui.components.PorterCard
import com.porter.core.ui.components.PorterPrimaryButton
import com.porter.core.ui.components.PorterTopBar
import com.porter.domain.model.DocumentType
import com.porter.domain.model.ShippingDocument
import com.porter.feature.documents.viewmodel.DocumentsViewModel

@Composable
fun DocumentsScreen(
    bookingId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DocumentsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(bookingId) {
        viewModel.loadDocuments(bookingId)
    }

    Scaffold(
        topBar = {
            PorterTopBar(
                title = "Shipping Documents",
                onNavigateBack = onBack
            )
        },
        containerColor = CanvasWhite,
        modifier = modifier
    ) { padding ->
        when (val state = uiState) {
            is UiState.Loading -> LoadingScreen(modifier = Modifier.padding(padding))
            is UiState.Error -> ErrorScreen(
                error = state.error,
                onRetry = { viewModel.loadDocuments(bookingId) },
                modifier = Modifier.padding(padding)
            )
            is UiState.Empty -> {
                EmptyDocumentsContent(
                    bookingId = bookingId,
                    onUpload = {
                        viewModel.uploadMockDocument(
                            bookingId = bookingId,
                            type = DocumentType.BILL_OF_LADING,
                            fileName = "BOL_${bookingId}.pdf"
                        )
                    },
                    modifier = Modifier.padding(padding)
                )
            }
            is UiState.Success -> {
                DocumentsListContent(
                    bookingId = bookingId,
                    documents = state.data,
                    onUpload = {
                        viewModel.uploadMockDocument(
                            bookingId = bookingId,
                            type = DocumentType.DELIVERY_ORDER,
                            fileName = "DeliveryOrder_${bookingId}.pdf"
                        )
                    },
                    onDelete = { viewModel.deleteDocument(it) },
                    modifier = Modifier.padding(padding)
                )
            }
            else -> {}
        }
    }
}

@Composable
private fun DocumentsListContent(
    bookingId: String,
    documents: List<ShippingDocument>,
    onUpload: () -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasWhite)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Shipment #$bookingId",
                style = Caption.copy(color = ActionBlue, fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = "Freight Documents (${documents.size})",
                style = com.porter.core.designsystem.theme.HeroDisplay.copy(
                    fontSize = 24.sp,
                    color = InkNearBlack
                )
            )
            Text(
                text = "Bill of Lading, Gate Pass, and Delivery Orders for port customs clearance.",
                style = BodyDefault.copy(color = InkMuted48)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(documents, key = { it.id }) { doc ->
            DocumentCard(doc = doc, onDelete = { onDelete(doc.id) })
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            PorterPrimaryButton(
                text = "+ Upload New Shipping Document",
                onClick = onUpload
            )
        }
    }
}

@Composable
private fun DocumentCard(
    doc: ShippingDocument,
    onDelete: () -> Unit
) {
    PorterCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Parchment, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = ActionBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.size(12.dp))

                Column {
                    Text(
                        text = doc.fileName,
                        style = BodyStrong.copy(fontSize = 15.sp, color = InkNearBlack)
                    )
                    Text(
                        text = "${doc.type.name.replace('_', ' ')} • ${(doc.fileSizeBytes / 1024)} KB",
                        style = FinePrint.copy(color = InkMuted48)
                    )
                    if (doc.isVerified) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified",
                                tint = StatusSuccess,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = " Port Verified",
                                style = FinePrint.copy(color = StatusSuccess, fontSize = 11.sp)
                            )
                        }
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = StatusError.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun EmptyDocumentsContent(
    bookingId: String,
    onUpload: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasWhite)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Description,
            contentDescription = null,
            tint = InkMuted48,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No documents uploaded yet",
            style = BodyStrong.copy(fontSize = 18.sp, color = InkNearBlack)
        )
        Text(
            text = "Upload Bill of Lading, Gate Pass, or Delivery Order for port handling.",
            style = Caption.copy(color = InkMuted48),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        PorterPrimaryButton(
            text = "Upload Document",
            onClick = onUpload
        )
    }
}
