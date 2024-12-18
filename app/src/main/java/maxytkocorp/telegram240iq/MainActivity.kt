package maxytkocorp.telegram240iq

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModelProvider
import coil.compose.AsyncImage
import maxytkocorp.telegram240iq.Models.Chat
import maxytkocorp.telegram240iq.Models.Message
import maxytkocorp.telegram240iq.Models.MessageData
import maxytkocorp.telegram240iq.ViewModels.Auth
import maxytkocorp.telegram240iq.ViewModels.AuthFactory
import maxytkocorp.telegram240iq.ViewModels.Main
import maxytkocorp.telegram240iq.ViewModels.MainFactory
import maxytkocorp.telegram240iq.Web.RetrofitInstance
import maxytkocorp.telegram240iq.Web.SessionManager
import maxytkocorp.telegram240iq.dal.AppDB
import maxytkocorp.telegram240iq.ui.theme.Telegram240IQTheme

class MainActivity : ComponentActivity() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)

        val database = AppDB.getInstance(this)
        val chatDao = database.chatDao()
        val messageDao = database.messageDao()
        val apiService = RetrofitInstance.apiService

        val chatRepository = ChatRepository(chatDao, messageDao, apiService)

        val auth = ViewModelProvider(
            this,
            AuthFactory(sessionManager)
        ).get(Auth::class.java)

        val main = ViewModelProvider(
            this,
            MainFactory(sessionManager, chatRepository)
        ).get(Main::class.java)

        setContent {
            Telegram240IQTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppScreen(auth, main)
                }
            }
        }
    }
}

@Composable
fun AppScreen(auth: Auth, main: Main) {
    val authState by auth.authState.collectAsState()

    when (authState) {
        is Auth.AuthState.LoggedIn -> {
            MainScreen(main)
        }

        else -> {
            AuthScreen(auth)
        }
    }
}

@Composable
fun AuthScreen(auth: Auth) {
    val authState by auth.authState.collectAsState()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = { auth.register(username) }) {
                Text("Register")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { auth.login(username, password) }) {
                Text("Login")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        when (authState) {
            is Auth.AuthState.Registered -> Text("Registered! Password: ${(authState as Auth.AuthState.Registered).password}")
            is Auth.AuthState.LoggedIn -> Text("Logged in! Token: ${(authState as Auth.AuthState.LoggedIn).token}")
            is Auth.AuthState.Error -> Text("Error: ${(authState as Auth.AuthState.Error).message}")
            else -> {}
        }
    }
}

@Composable
fun MainScreen(
    main: Main,
) {
    val state by main.state.collectAsState()
    val selectedChatOrChannel by main.selectedChatOrChannel.collectAsState()
    val messages by main.messages.collectAsState()

    LaunchedEffect(Unit) {
        main.loadChats()
    }

    BackHandler {
        when {
            selectedChatOrChannel != null -> {
                main.selectChat(null)
            }

            else -> {
                main.logout()
            }
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                ChatListWithTonButton(
                    state = state,
                    onChatSelected = { main.selectChat(it) }
                )
            }
            Box(modifier = Modifier.weight(2f)) {
                if (selectedChatOrChannel != null) {
                    val chatOrChannel = selectedChatOrChannel!!
                    ChatDetailScreen(
                        chat = chatOrChannel,
                        onBack = { main.selectChat(null) },
                        messages = messages,
                        onSendMessage = { message ->
                            main.sendMessage(chatOrChannel.name, message)
                        },
                        onLoadMoreMessages = {
                            main.loadMoreMessages(chatOrChannel)
                        }
                    )
                } else {
                    Text(
                        stringResource(R.string.choose_chat),
                        modifier = Modifier
                            .fillMaxSize()
                            .wrapContentSize(),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            if (selectedChatOrChannel == null) {
                ChatListWithTonButton(
                    state = state,
                    onChatSelected = { main.selectChat(it) }
                )
            } else {
                val chatOrChannel = selectedChatOrChannel!!
                ChatDetailScreen(
                    chat = chatOrChannel,
                    onBack = { main.selectChat(null) },
                    messages = messages,
                    onSendMessage = { message ->
                        main.sendMessage(chatOrChannel.name, message)
                    },
                    onLoadMoreMessages = {
                        main.loadMoreMessages(chatOrChannel)
                    }
                )
            }
        }
    }
}


@Composable
fun CustomLoadingSpinner() {
    var rotationAngle by remember { mutableFloatStateOf(0f) }

    // Create an animation that updates the rotation angle
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Canvas(modifier = Modifier.size(500.dp), onDraw = {
        val center = this.center

        val starPath = Path().apply {
            moveTo(center.x, center.y - 100f)
            lineTo(center.x + 60f, center.y + 60f)
            lineTo(center.x - 120f, center.y - 60f)
            lineTo(center.x + 120f, center.y - 60f)
            lineTo(center.x - 60f, center.y + 60f)
            close()
        }

        rotate(rotation) {
            drawPath(
                path = starPath, brush = Brush.linearGradient(
                    colors = listOf(Color.Blue, Color.Cyan),
                    start = center,
                    end = center.copy(x = center.x + 100f, y = center.y + 100f),
                    tileMode = TileMode.Repeated
                )
            )
        }

        drawPath(path = starPath, color = Color.Cyan.copy(alpha = 0.5f), style = Fill)
    })
}


@Composable
fun ChatList(
    state: Main.MainScreenState,
    onChatSelected: (Chat) -> Unit,
) {
    when (state) {
        is Main.MainScreenState.Loading -> {
            CustomLoadingSpinner()
        }

        is Main.MainScreenState.Success -> {
            val items = state.chatsAndChannels
            LazyColumn {
                items(items) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChatSelected(item) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.chat),
                            contentDescription = "Chat Icon",
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 8.dp) // Отступ справа от иконки
                        )
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        is Main.MainScreenState.Error -> {
            Text(
                stringResource(R.string.error, state.message),
                modifier = Modifier.fillMaxSize(),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun ChatListWithTonButton(
    state: Main.MainScreenState,
    onChatSelected: (Chat) -> Unit,
) {

    var tonCount by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TON: $tonCount",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = { tonCount++ }) {
                Text(text = "Add TON")
            }
        }

        when (state) {
            is Main.MainScreenState.Loading -> {
                CustomLoadingSpinner()
            }

            is Main.MainScreenState.Success -> {
                val items = state.chatsAndChannels
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(items) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onChatSelected(item) }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.chat),
                                contentDescription = "Chat Icon",
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(end = 8.dp)
                            )

                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            is Main.MainScreenState.Error -> {
                Text(
                    text = stringResource(R.string.error, state.message),
                    modifier = Modifier.fillMaxSize(),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}



@Composable
fun ChatDetailScreen(
    chat: Chat,
    onBack: () -> Unit,
    messages: List<Message>,
    onSendMessage: (String) -> Unit,
    onLoadMoreMessages: suspend () -> Boolean,
) {
    var inputMessage by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var canLoadMore by remember { mutableStateOf(true) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty() && listState.firstVisibleItemIndex == 0) {
            listState.scrollToItem(0)
        }
    }

    LaunchedEffect(listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index, canLoadMore) {
        val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
        if (lastVisibleIndex == messages.lastIndex && canLoadMore) {
            canLoadMore = onLoadMoreMessages()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Chat: ${chat.name}",
                style = MaterialTheme.typography.titleMedium
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            reverseLayout = true
        ) {
            items(messages) { message ->
                when (val data = message.data) {
                    is MessageData.Text -> Text("${message.from}: ${data.text}")
                    is MessageData.Image -> {
                        data.link?.let {
                            ImageMessage(
                                from = message.from,
                                imagePath = it
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = inputMessage,
                onValueChange = { inputMessage = it },
                placeholder = { Text(stringResource(R.string.type_a_message)) },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                maxLines = 1,
                singleLine = true
            )
            Button(
                onClick = {
                    if (inputMessage.isNotBlank()) {
                        onSendMessage(inputMessage)
                        inputMessage = ""
                    }
                },
                enabled = inputMessage.isNotBlank()
            ) {
                Text(stringResource(R.string.send_button))
            }
        }
    }
}

@Composable
fun ImageMessage(
    from: String,
    imagePath: String,
) {
    var showFullScreenImage by remember { mutableStateOf(false) }

    Column {
        Text("$from: ", style = MaterialTheme.typography.bodyMedium)

        AsyncImage(
            model = stringResource(R.string.img_url, "thumb/$imagePath"),
            contentDescription = "Thumbnail",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clickable { showFullScreenImage = true }
        )
    }

    if (showFullScreenImage) {
        FullScreenImageDialog(
            imageUrl = stringResource(R.string.img_url, "img/$imagePath"),
            onClose = { showFullScreenImage = false }
        )
    }
}

@Composable
fun FullScreenImageDialog(
    imageUrl: String,
    onClose: () -> Unit,
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Full-size image",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
            )

            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(24.dp)
                    .clickable { onClose() }
            )
        }
    }
}
