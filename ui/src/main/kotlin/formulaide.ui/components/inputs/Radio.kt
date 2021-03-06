package formulaide.ui.components.inputs

import formulaide.ui.utils.classes
import react.FC
import react.Props
import react.dom.html.InputType
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label

external interface RadioButtonProps : Props {
	/**
	 * Unique ID common to all buttons in this group.
	 */
	var radioId: String

	/**
	 * Unique ID of this button.
	 */
	var buttonId: String

	/**
	 * The value sent to the server in case this button is selected.
	 *
	 * To check whether this button is selected or not, see [checked].
	 */
	var value: String
	var text: String

	var checked: Boolean?
	var onClick: (() -> Unit)?
}

/**
 * Multiple buttons that act in a group.
 *
 * Only one button in the group can be selected at once.
 */
val RadioButton = FC<RadioButtonProps>("RadioButton") { props ->
	input {
		this.type = InputType.radio
		name = props.radioId
		classes = "mr-1"
		id = props.buttonId
		this.value = props.value

		onChange = { props.onClick?.invoke() }
		this.checked = props.checked
	}

	label {
		classes = "mr-2"

		+props.text
		htmlFor = props.buttonId
	}
}
