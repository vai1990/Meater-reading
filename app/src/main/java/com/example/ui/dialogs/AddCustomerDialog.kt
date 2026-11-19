package com.example.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.Customer

@Composable
fun AddCustomerDialog(
    customerToEdit: Customer?,
    isHindi: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        address: String,
        meterNumber: String,
        consumerNumber: String,
        phoneNumber: String,
        connectionType: String,
        sanctionedLoad: String,
        existingId: Long
    ) -> Unit
) {
    var name by remember { mutableStateOf(customerToEdit?.name ?: "") }
    var address by remember { mutableStateOf(customerToEdit?.address ?: "") }
    var meterNumber by remember { mutableStateOf(customerToEdit?.meterNumber ?: "") }
    var consumerNumber by remember { mutableStateOf(customerToEdit?.consumerNumber ?: "") }
    var phoneNumber by remember { mutableStateOf(customerToEdit?.phoneNumber ?: "") }
    var connectionType by remember { mutableStateOf(customerToEdit?.connectionType ?: "3-Phase Commercial") }
    var sanctionedLoad by remember { mutableStateOf(customerToEdit?.sanctionedLoad ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("add_customer_dialog"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (customerToEdit != null) Icons.Default.Business else Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (customerToEdit != null) {
                        if (isHindi) "ग्राहक विवरण एडिट करें" else "Edit Customer Profile"
                    } else {
                        if (isHindi) "नया ग्राहक जोड़ें (Add Customer)" else "Add New Customer"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Customer Name
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text(if (isHindi) "ग्राहक का नाम (Customer Name) *" else "Customer Name *") },
                    placeholder = { Text("उदा. मेसर्स शर्मा इंडस्ट्रीज") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_name_input"),
                    singleLine = true
                )

                // Customer Address
                OutlinedTextField(
                    value = address,
                    onValueChange = {
                        address = it
                        errorMessage = null
                    },
                    label = { Text(if (isHindi) "ग्राहक का पता (Customer Address) *" else "Customer Address *") },
                    placeholder = { Text("उदा. प्लॉट नं 12, एमआईडीसी, पुणे") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_address_input"),
                    maxLines = 3
                )

                // Meter Number
                OutlinedTextField(
                    value = meterNumber,
                    onValueChange = {
                        meterNumber = it
                        errorMessage = null
                    },
                    label = { Text(if (isHindi) "मीटर नंबर (Meter No.) *" else "Meter Number *") },
                    placeholder = { Text("उदा. MTR-98234") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_meter_number_input"),
                    singleLine = true
                )

                // Consumer / CA Number
                OutlinedTextField(
                    value = consumerNumber,
                    onValueChange = { consumerNumber = it },
                    label = { Text(if (isHindi) "उपभोक्ता संख्या (CA / Account No.)" else "Consumer / CA No.") },
                    placeholder = { Text("उदा. 100239482") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_ca_number_input"),
                    singleLine = true
                )

                // Mobile Number
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text(if (isHindi) "मोबाइल नंबर (Mobile No.)" else "Mobile Number") },
                    placeholder = { Text("+91 98765 00000") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_phone_input"),
                    singleLine = true
                )

                // Connection Type & Sanctioned Load
                OutlinedTextField(
                    value = connectionType,
                    onValueChange = { connectionType = it },
                    label = { Text(if (isHindi) "कनेक्शन प्रकार (Connection Type)" else "Connection Type") },
                    placeholder = { Text("3-Phase Industrial / LT Commercial / HT") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_connection_type_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = sanctionedLoad,
                    onValueChange = { sanctionedLoad = it },
                    label = { Text(if (isHindi) "स्वीकृत भार (Sanctioned Load)" else "Sanctioned Load (e.g. 50 kW)") },
                    placeholder = { Text("उदा. 50 kW / 80 kVA") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_load_input"),
                    singleLine = true
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = if (isHindi) "कृपया ग्राहक का नाम दर्ज करें" else "Please enter customer name"
                        return@Button
                    }
                    if (address.isBlank()) {
                        errorMessage = if (isHindi) "कृपया ग्राहक का पता दर्ज करें" else "Please enter customer address"
                        return@Button
                    }
                    if (meterNumber.isBlank()) {
                        errorMessage = if (isHindi) "कृपया मीटर नंबर दर्ज करें" else "Please enter meter number"
                        return@Button
                    }

                    onSave(
                        name.trim(),
                        address.trim(),
                        meterNumber.trim(),
                        consumerNumber.trim(),
                        phoneNumber.trim(),
                        connectionType.trim(),
                        sanctionedLoad.trim(),
                        customerToEdit?.id ?: 0L
                    )
                },
                modifier = Modifier.testTag("save_customer_button")
            ) {
                Text(if (isHindi) "सेव करें (Save)" else "Save Customer")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_customer_button")
            ) {
                Text(if (isHindi) "रद्द करें" else "Cancel")
            }
        }
    )
}
