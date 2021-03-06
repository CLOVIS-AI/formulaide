package formulaide.ui.components

import formulaide.ui.*
import formulaide.ui.components.cards.CardShell
import formulaide.ui.components.text.LightText
import formulaide.ui.components.text.Title
import formulaide.ui.utils.classes
import formulaide.ui.utils.useGlobalState
import kotlinx.coroutines.delay
import react.FC
import react.Props
import react.dom.html.ReactHTML.div

/**
 * The app's top bar, with the title, the logout button and the navigation buttons.
 */
val TopBar = FC<Props>("TopBar") {
	val user by useUser()

	val subtitle = when (user) {
		null -> "Accès anonyme"
		else -> "Bonjour ${user!!.fullName}"
	}

	var notification by useGlobalState(lastNotification)
	useAsyncEffect(notification) {
		delay(NOTIFICATION_LIFETIME_SECONDS * 1000)
		notification = null
	}

	CardShell {
		div {
			classes = "flex flex-col-reverse justify-center md:flex-row md:justify-between md:items-center"

			div {
				Navigation()
			}

			div {
				classes = "mb-2 md:mb-0"

				Title { title = "Formulaide" }
				LightText { text = subtitle }

				if (user != null) StyledButton {
					text = "×"
					action = {
						logout()
						navigateTo(Screen.Home)
					}
				}
			}
		}
	}

	if (notification != null)
		ConfirmationNotification {
			this.notification = notification!!
		}
}
