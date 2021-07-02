package formulaide.ui.components

import formulaide.ui.utils.text
import kotlinx.html.DIV
import kotlinx.html.FORM
import react.RBuilder
import react.dom.*

private fun RBuilder.styledCardTitle(title: String, secondary: String?) {
	h2("text-xl") { text(title) }
	if (secondary != null) p("text-gray-600") { text(secondary) }
}

private fun RBuilder.styledCardShell(contents: RDOMBuilder<DIV>.() -> Unit) =
	div("m-4 p-4 shadow-xl rounded-lg") {
		contents()
	}

fun RBuilder.styledCard(
	title: String,
	secondary: String?,
	vararg actions: Pair<String, () -> Unit>,
	contents: RBuilder.() -> Unit,
) {
	styledCardShell {
		styledCardTitle(title, secondary)

		div("py-4") {
			contents()
		}

		for (action in actions) {
			styledButton(action.first, default = action == actions.first()) { action.second() }
		}
	}
}

fun RBuilder.styledFormCard(
	title: String,
	secondary: String?,
	submit: String,
	vararg actions: Pair<String, () -> Unit>,
	contents: RBuilder.() -> Unit,
	handler: FORM.() -> Unit,
) {
	styledCardShell {
		form {
			styledCardTitle(title, secondary)

			div("py-4") {
				contents()
			}

			styledSubmit(submit, default = true)
			for (action in actions) {
				styledButton(action.first, default = false) { action.second() }
			}

			attrs(handler)
		}
	}
}
