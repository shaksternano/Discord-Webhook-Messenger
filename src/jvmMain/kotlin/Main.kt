import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.launch

@Composable
fun app() {
    val webhookUrlState = remember { mutableStateOf(TextFieldValue()) }
    val messageState = remember { mutableStateOf(TextFieldValue()) }
    val usernameState = remember { mutableStateOf(TextFieldValue()) }
    val avatarUrlState = remember { mutableStateOf(TextFieldValue()) }

    val webhookUrlErrorState = remember { mutableStateOf("") }
    val missingMessageState = remember { mutableStateOf("") }

    val sentState = remember { mutableStateOf("Send") }

    val sentMessageErrorState = remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()

    MaterialTheme {
        Column(Modifier.fillMaxSize(), Arrangement.spacedBy(5.dp)) {
            TextField(
                value = webhookUrlState.value,
                onValueChange = {
                    webhookUrlState.value = it
                    webhookUrlErrorState.value = ""
                    sentState.value = "Send"
                },
                label = { Text("Webhook URL (required)") },
                trailingIcon = {
                    if (webhookUrlErrorState.value.isNotBlank()) {
                        Icon(Icons.Filled.Info, webhookUrlErrorState.value, tint = MaterialTheme.colors.error)
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            if (webhookUrlErrorState.value.isNotBlank()) {
                Text(
                    text = webhookUrlErrorState.value,
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }

            TextField(
                value = messageState.value,
                onValueChange = {
                    messageState.value = it
                    missingMessageState.value = ""
                    sentState.value = "Send"
                },
                label = { Text("Message (required)") },
                trailingIcon = {
                    if (missingMessageState.value.isNotBlank()) {
                        Icon(Icons.Filled.Info, missingMessageState.value, tint = MaterialTheme.colors.error)
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            if (missingMessageState.value.isNotBlank()) {
                Text(
                    text = missingMessageState.value,
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }

            TextField(
                value = usernameState.value,
                onValueChange = {
                    usernameState.value = it
                    sentState.value = "Send"
                },
                label = { Text("Username") },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            TextField(
                value = avatarUrlState.value,
                onValueChange = {
                    avatarUrlState.value = it
                    sentState.value = "Send"
                },
                label = { Text("Avatar URL") },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            Button(
                onClick = {
                    sentMessageErrorState.value = ""
                    if (webhookUrlState.value.text.isBlank()) {
                        webhookUrlErrorState.value = "Missing webhook URL"
                    }
                    if (messageState.value.text.isBlank()) {
                        missingMessageState.value = "Missing message"
                    }
                    if (webhookUrlErrorState.value.isBlank() && missingMessageState.value.isBlank()) {
                        sentState.value = "Sending..."
                        coroutineScope.launch {
                            try {
                                val response = sendWebhookMessage(
                                    webhookUrlState.value.text,
                                    messageState.value.text,
                                    usernameState.value.text,
                                    avatarUrlState.value.text
                                )
                                val responseCode = response.status.value
                                sentMessageErrorState.value = when {
                                    responseCode >= 500 -> "Error with Discord"
                                    responseCode >= 400 -> "Error with one or more fields"
                                    responseCode in 200..299 -> ""
                                    else -> "Error sending message"
                                }

                                if (responseCode in 200..299) {
                                    sentState.value = "Sent"
                                    messageState.value = TextFieldValue("")
                                } else {
                                    sentState.value = "Error"
                                }
                            } catch (e: Exception) {
                                sentMessageErrorState.value = "Error sending message"
                                sentState.value = "Error"
                            }
                        }
                    } else {
                        sentState.value = "Error"
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(sentState.value)
            }

            if (sentMessageErrorState.value.isNotBlank()) {
                Text(
                    text = sentMessageErrorState.value,
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }
}

suspend fun sendWebhookMessage(
    webhookUrl: String,
    message: String,
    username: String = "",
    avatarUrl: String = ""
): HttpResponse {
    var body = "{" +
            "\"content\":\"${message}\""
    if (username.isNotBlank()) {
        body += ",\"username\":\"${username}\""
    }
    if (avatarUrl.isNotBlank()) {
        body += ",\"avatar_url\":\"${avatarUrl}\""
    }
    body += "}"
    val client = HttpClient()
    return client.post(webhookUrl) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Discord Webhook Messenger",
        state = rememberWindowState(width = 800.dp, height = 450.dp)
    ) {
        app()
    }
}
