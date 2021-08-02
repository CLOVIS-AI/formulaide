package formulaide.ui.components

import formulaide.ui.utils.text
import kotlinx.html.INPUT
import kotlinx.html.InputType
import kotlinx.html.SELECT
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.HTMLSelectElement
import react.RBuilder
import react.dom.*
import react.Ref as RRef

private const val commonInputStyle =
	"rounded bg-gray-200 border-b-2 border-gray-400 focus:border-purple-800 my-1 focus:outline-none"
private const val largeInputStyle = "$commonInputStyle w-60 mr-3"
private const val smallInputStyle = "$commonInputStyle w-10"

fun RBuilder.styledField(
	id: String,
	displayName: String,
	contents: RBuilder.() -> Unit,
) {
	styledFormField {
		label("block") {
			attrs["htmlFor"] = id

			text(displayName)
		}

		contents()
	}
}

fun RBuilder.styledInput(
	type: InputType,
	id: String,
	required: Boolean = false,
	ref: RRef? = null,
	handler: INPUT.() -> Unit = {},
) {
	input(type, classes = largeInputStyle) {
		attrs {
			this.id = id
			this.name = id
			this.required = required

			handler()
		}

		if (ref != null) this.ref = ref
	}
	if (required)
		text(" *")
}

fun RBuilder.styledSmallInput(
	type: InputType,
	id: String,
	required: Boolean = false,
	ref: RRef? = null,
	handler: INPUT.() -> Unit = {},
) {
	input(type, classes = smallInputStyle) {
		attrs {
			this.id = id
			this.name = id
			this.required = required

			handler()
		}

		if (ref != null) this.ref = ref
	}
}

fun RBuilder.styledFormField(contents: RBuilder.() -> Unit) {
	div("mb-2") {
		contents()
	}
}

fun RBuilder.styledRadioButton(
	radioId: String,
	buttonId: String,
	value: String,
	text: String,
	checked: Boolean = false,
	onClick: () -> Unit = {},
) {
	input(InputType.radio, name = radioId, classes = "mr-1") {
		attrs {
			this.id = buttonId
			this.value = value
			this.checked = checked

			onChangeFunction = { onClick() }
		}
	}

	label(classes = "mr-2") {
		text(text)
		attrs["htmlFor"] = buttonId
	}
}

fun RBuilder.styledCheckbox(
	id: String,
	message: String,
	required: Boolean = false,
	ref: RRef? = null,
	handler: INPUT.() -> Unit = {},
) {
	input(InputType.hidden, name = id) {
		attrs { value = "false" }
	}

	styledSmallInput(InputType.checkBox, id, required, ref) {
		value = "true"
		handler()
	}
	label {
		text(message)
		attrs["htmlFor"] = id
	}
}

fun RBuilder.styledSelect(
	handler: SELECT.() -> Unit = {},
	onSelect: (HTMLSelectElement) -> Unit = {},
	contents: RDOMBuilder<SELECT>.() -> Unit,
) {
	select(largeInputStyle) {
		attrs {
			onChangeFunction = { onSelect(it.target as HTMLSelectElement) }

			handler()
		}

		contents()
	}
}