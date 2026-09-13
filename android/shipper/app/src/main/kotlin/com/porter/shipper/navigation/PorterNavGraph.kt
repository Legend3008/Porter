package com.porter.shipper.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.porter.feature.auth.screen.LoginScreen
import com.porter.feature.auth.screen.OnboardingScreen
import com.porter.feature.auth.screen.OtpScreen
import com.porter.feature.auth.screen.RegistrationScreen
import com.porter.feature.auth.screen.SplashScreen
import com.porter.feature.booking.screen.BookingConfirmationScreen
import com.porter.feature.booking.screen.BookingDetailScreen
import com.porter.feature.booking.screen.BookingsListScreen
import com.porter.feature.booking.screen.BookingReviewScreen
import com.porter.feature.booking.screen.ContainerDetailsScreen
import com.porter.feature.booking.screen.CreateShipmentScreen
import com.porter.feature.booking.screen.PaymentProcessingScreen
import com.porter.feature.booking.screen.PaymentScreen
import com.porter.feature.booking.screen.PickupDeliveryScreen
import com.porter.feature.booking.screen.QuoteScreen
import com.porter.feature.documents.screen.DocumentsScreen
import com.porter.feature.home.screen.HomeScreen
import com.porter.feature.notifications.screen.NotificationsScreen
import com.porter.feature.payments.screen.InvoiceDetailScreen
import com.porter.feature.payments.screen.InvoicesScreen
import com.porter.feature.payments.screen.PaymentHistoryScreen
import com.porter.feature.profile.screen.AccountDeletionScreen
import com.porter.feature.profile.screen.ProfileScreen
import com.porter.feature.profile.screen.SettingsScreen
import com.porter.feature.profile.screen.SupportScreen
import com.porter.feature.tracking.screen.LiveTrackingScreen
import com.porter.feature.tracking.screen.StatusTimelineScreen

// ─── Route constants ───────────────────────────────────────────────────────────

object PorterRoutes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val OTP = "otp/{phone}"
    fun otp(phone: String) = "otp/$phone"

    const val HOME = "home"

    // Booking flow
    const val CREATE_SHIPMENT = "create_shipment"
    const val CONTAINER_DETAILS = "container_details"
    const val PICKUP_DELIVERY = "pickup_delivery"
    const val QUOTE = "quote"
    const val BOOKING_REVIEW = "booking_review"
    const val PAYMENT = "payment/{bookingId}"
    fun payment(bookingId: String) = "payment/$bookingId"
    const val PAYMENT_PROCESSING = "payment_processing/{bookingId}"
    fun paymentProcessing(bookingId: String) = "payment_processing/$bookingId"
    const val BOOKING_CONFIRMATION = "booking_confirmation/{bookingId}"
    fun bookingConfirmation(bookingId: String) = "booking_confirmation/$bookingId"
    const val BOOKINGS_LIST = "bookings_list"
    const val BOOKING_DETAIL = "booking_detail/{bookingId}"
    fun bookingDetail(bookingId: String) = "booking_detail/$bookingId"

    // Tracking
    const val LIVE_TRACKING = "live_tracking/{tripId}"
    fun liveTracking(tripId: String) = "live_tracking/$tripId"
    const val STATUS_TIMELINE = "status_timeline/{bookingId}"
    fun statusTimeline(bookingId: String) = "status_timeline/$bookingId"

    // Documents
    const val DOCUMENTS = "documents/{bookingId}"
    fun documents(bookingId: String) = "documents/$bookingId"

    // Payments
    const val INVOICES = "invoices"
    const val INVOICE_DETAIL = "invoice_detail/{invoiceId}"
    fun invoiceDetail(invoiceId: String) = "invoice_detail/$invoiceId"
    const val PAYMENT_HISTORY = "payment_history"

    // Notifications
    const val NOTIFICATIONS = "notifications"

    // Profile
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val SUPPORT = "support"
    const val ACCOUNT_DELETION = "account_deletion"
}

// ─── Nav Graph ─────────────────────────────────────────────────────────────────

@Composable
fun PorterNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = PorterRoutes.SPLASH
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
            ) + fadeIn(
                animationSpec = tween(durationMillis = 300)
            ) + scaleIn(
                initialScale = 0.94f,
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
            ) + fadeOut(
                animationSpec = tween(durationMillis = 220)
            ) + scaleOut(
                targetScale = 0.96f,
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
            ) + fadeIn(
                animationSpec = tween(durationMillis = 300)
            ) + scaleIn(
                initialScale = 0.96f,
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
            ) + fadeOut(
                animationSpec = tween(durationMillis = 220)
            ) + scaleOut(
                targetScale = 0.94f,
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
            )
        }
    ) {
        // ── Auth ──────────────────────────────────────────────────────────────
        composable(
            route = PorterRoutes.SPLASH,
            exitTransition = { fadeOut(animationSpec = tween(durationMillis = 400)) }
        ) {
            SplashScreen(
                onNavigateToOnboarding = {
                    navController.navigate(PorterRoutes.ONBOARDING) {
                        popUpTo(PorterRoutes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(PorterRoutes.HOME) {
                        popUpTo(PorterRoutes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(PorterRoutes.ONBOARDING) {
            OnboardingScreen(
                onGetStarted = { navController.navigate(PorterRoutes.LOGIN) }
            )
        }

        composable(PorterRoutes.LOGIN) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(PorterRoutes.REGISTER) },
                onNavigateToOtp = { phone -> navController.navigate(PorterRoutes.otp(phone)) },
                onNavigateToHome = {
                    navController.navigate(PorterRoutes.HOME) {
                        popUpTo(PorterRoutes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(PorterRoutes.REGISTER) {
            RegistrationScreen(
                onNavigateToOtp = { phone -> navController.navigate(PorterRoutes.otp(phone)) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = PorterRoutes.OTP,
            arguments = listOf(navArgument("phone") { type = NavType.StringType })
        ) { backStack ->
            val phone = backStack.arguments?.getString("phone") ?: ""
            OtpScreen(
                phone = phone,
                onVerified = {
                    navController.navigate(PorterRoutes.HOME) {
                        popUpTo(PorterRoutes.ONBOARDING) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Home ──────────────────────────────────────────────────────────────
        composable(PorterRoutes.HOME) {
            HomeScreen(
                onCreateShipment = { navController.navigate(PorterRoutes.CREATE_SHIPMENT) },
                onViewBookings = { navController.navigate(PorterRoutes.BOOKINGS_LIST) },
                onOpenBooking = { id -> navController.navigate(PorterRoutes.bookingDetail(id)) },
                onOpenNotifications = { navController.navigate(PorterRoutes.NOTIFICATIONS) },
                onOpenProfile = { navController.navigate(PorterRoutes.PROFILE) },
                onOpenInvoices = { navController.navigate(PorterRoutes.INVOICES) }
            )
        }

        // ── Booking flow ──────────────────────────────────────────────────────
        composable(PorterRoutes.CREATE_SHIPMENT) {
            CreateShipmentScreen(
                onNext = { navController.navigate(PorterRoutes.CONTAINER_DETAILS) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(PorterRoutes.CONTAINER_DETAILS) {
            ContainerDetailsScreen(
                onNext = { navController.navigate(PorterRoutes.PICKUP_DELIVERY) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(PorterRoutes.PICKUP_DELIVERY) {
            PickupDeliveryScreen(
                onNext = { navController.navigate(PorterRoutes.QUOTE) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(PorterRoutes.QUOTE) {
            QuoteScreen(
                onProceed = { navController.navigate(PorterRoutes.BOOKING_REVIEW) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(PorterRoutes.BOOKING_REVIEW) {
            BookingReviewScreen(
                onConfirm = { bookingId -> navController.navigate(PorterRoutes.payment(bookingId)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = PorterRoutes.PAYMENT,
            arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
        ) { backStack ->
            val bookingId = backStack.arguments?.getString("bookingId") ?: ""
            PaymentScreen(
                bookingId = bookingId,
                onPaymentInitiated = { navController.navigate(PorterRoutes.paymentProcessing(bookingId)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = PorterRoutes.PAYMENT_PROCESSING,
            arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
        ) { backStack ->
            val bookingId = backStack.arguments?.getString("bookingId") ?: ""
            PaymentProcessingScreen(
                bookingId = bookingId,
                onPaymentConfirmed = {
                    navController.navigate(PorterRoutes.bookingConfirmation(bookingId)) {
                        popUpTo(PorterRoutes.CREATE_SHIPMENT) { inclusive = true }
                    }
                },
                onPaymentFailed = { navController.popBackStack() }
            )
        }

        composable(
            route = PorterRoutes.BOOKING_CONFIRMATION,
            arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
        ) { backStack ->
            val bookingId = backStack.arguments?.getString("bookingId") ?: ""
            BookingConfirmationScreen(
                bookingId = bookingId,
                onTrack = { tripId -> navController.navigate(PorterRoutes.liveTracking(tripId)) },
                onGoHome = {
                    navController.navigate(PorterRoutes.HOME) {
                        popUpTo(PorterRoutes.HOME) { inclusive = true }
                    }
                }
            )
        }

        composable(PorterRoutes.BOOKINGS_LIST) {
            BookingsListScreen(
                onOpenBooking = { id -> navController.navigate(PorterRoutes.bookingDetail(id)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = PorterRoutes.BOOKING_DETAIL,
            arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
        ) { backStack ->
            val bookingId = backStack.arguments?.getString("bookingId") ?: ""
            BookingDetailScreen(
                bookingId = bookingId,
                onTrack = { tripId -> navController.navigate(PorterRoutes.liveTracking(tripId)) },
                onTimeline = { navController.navigate(PorterRoutes.statusTimeline(bookingId)) },
                onDocuments = { navController.navigate(PorterRoutes.documents(bookingId)) },
                onBack = { navController.popBackStack() }
            )
        }

        // ── Tracking ──────────────────────────────────────────────────────────
        composable(
            route = PorterRoutes.LIVE_TRACKING,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType })
        ) { backStack ->
            val tripId = backStack.arguments?.getString("tripId") ?: ""
            LiveTrackingScreen(
                tripId = tripId,
                onTimeline = { navController.navigate(PorterRoutes.statusTimeline(tripId)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = PorterRoutes.STATUS_TIMELINE,
            arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
        ) { backStack ->
            val bookingId = backStack.arguments?.getString("bookingId") ?: ""
            StatusTimelineScreen(
                bookingId = bookingId,
                onBack = { navController.popBackStack() }
            )
        }

        // ── Documents ─────────────────────────────────────────────────────────
        composable(
            route = PorterRoutes.DOCUMENTS,
            arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
        ) { backStack ->
            val bookingId = backStack.arguments?.getString("bookingId") ?: ""
            DocumentsScreen(
                bookingId = bookingId,
                onBack = { navController.popBackStack() }
            )
        }

        // ── Payments ──────────────────────────────────────────────────────────
        composable(PorterRoutes.INVOICES) {
            InvoicesScreen(
                onOpenInvoice = { id -> navController.navigate(PorterRoutes.invoiceDetail(id)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = PorterRoutes.INVOICE_DETAIL,
            arguments = listOf(navArgument("invoiceId") { type = NavType.StringType })
        ) { backStack ->
            val invoiceId = backStack.arguments?.getString("invoiceId") ?: ""
            InvoiceDetailScreen(
                invoiceId = invoiceId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(PorterRoutes.PAYMENT_HISTORY) {
            PaymentHistoryScreen(onBack = { navController.popBackStack() })
        }

        // ── Notifications ─────────────────────────────────────────────────────
        composable(PorterRoutes.NOTIFICATIONS) {
            NotificationsScreen(
                onOpenBooking = { id -> navController.navigate(PorterRoutes.bookingDetail(id)) },
                onBack = { navController.popBackStack() }
            )
        }

        // ── Profile ───────────────────────────────────────────────────────────
        composable(PorterRoutes.PROFILE) {
            ProfileScreen(
                onSettings = { navController.navigate(PorterRoutes.SETTINGS) },
                onSupport = { navController.navigate(PorterRoutes.SUPPORT) },
                onInvoices = { navController.navigate(PorterRoutes.INVOICES) },
                onPaymentHistory = { navController.navigate(PorterRoutes.PAYMENT_HISTORY) },
                onAccountDeletion = { navController.navigate(PorterRoutes.ACCOUNT_DELETION) },
                onLogout = {
                    navController.navigate(PorterRoutes.LOGIN) {
                        popUpTo(PorterRoutes.HOME) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(PorterRoutes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(PorterRoutes.SUPPORT) {
            SupportScreen(onBack = { navController.popBackStack() })
        }

        composable(PorterRoutes.ACCOUNT_DELETION) {
            AccountDeletionScreen(
                onDeleted = {
                    navController.navigate(PorterRoutes.ONBOARDING) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
