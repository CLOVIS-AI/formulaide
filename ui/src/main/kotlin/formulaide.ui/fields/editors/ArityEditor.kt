package formulaide.ui.fields.editors

import formulaide.api.data.Composite
import formulaide.api.fields.*
import formulaide.api.fields.DeepFormField.Companion.createMatchingFormField
import formulaide.api.types.*
import formulaide.api.types.Ref.Companion.loadIfNecessary
import formulaide.ui.components.StyledButton
import formulaide.ui.components.inputs.Input
import formulaide.ui.useComposites
import react.FC
import react.dom.html.InputType
import kotlin.math.max
import kotlin.math.min

val ArityEditor = FC<EditableFieldProps>("ArityEditor") { props ->
	val field = props.field
	val arity = field.arity

	val composites by useComposites()

	//region Allowed
	// Editing the arity is allowed for all types but Message
	val simpleOrNull = (field as? Field.Simple)?.simple
	val allowModifications =
		if (simpleOrNull != null) simpleOrNull::class != SimpleField.Message::class
		else true
	//endregion
	//region Allowed range

	val minAllowedRange = when (field) {
		is DataField.Composite -> 0..0 // composite references can never be mandatory (see DataField.Composite)
		is DeepFormField -> field.dataField.arity.min..Int.MAX_VALUE
		else -> 0..Int.MAX_VALUE
	}

	val maxAllowedRange = when (field) {
		is ShallowFormField.Composite -> 0..Int.MAX_VALUE
		is DataField, is ShallowFormField -> 1..Int.MAX_VALUE
		is DeepFormField -> 0..field.dataField.arity.max
		else -> 0..Int.MAX_VALUE
	}

	//endregion

	if (allowModifications) {
		val modelArities = mapOf(
			Arity.forbidden() to "Caché",
			Arity.optional() to "Facultatif",
			Arity.mandatory() to "Obligatoire",
		).filterKeys { it.min in minAllowedRange && it.max in maxAllowedRange }

		for ((modelArity, arityName) in modelArities) {
			if (modelArity == arity) {
				StyledButton {
					text = arityName
					enabled = false
				}
			} else {
				StyledButton {
					text = arityName
					action = { updateSubFieldsOnMaxArityChange(props, modelArity, composites) }
				}
			}
		}

		if (arity.max > 1) {
			+"De "
			if (field !is DataField.Composite) {
				Input {
					type = InputType.number
					id = "item-arity-min-${props.uniqueId}"
					required = true
					value = arity.min.toString()
					min = minAllowedRange.first.toDouble()
					max = min(arity.max, minAllowedRange.last).toDouble()
					onChange = {
						val value = it.target.value.toInt()
						props.replace(
							props.field.requestCopy(arity = Arity(value, arity.max))
						)
					}
				}
			} else {
				+arity.min.toString()
			}
			+" à "
			Input {
				type = InputType.number
				id = "item-arity-min-${props.uniqueId}"
				required = true
				value = arity.max.toString()
				min = max(arity.min, max(maxAllowedRange.first, 2)).toDouble()
				max = maxAllowedRange.last.toDouble()
				onChange = {
					val value = it.target.value.toInt()
					updateSubFieldsOnMaxArityChange(props, Arity(arity.min, value), composites)
				}
			}
			+" réponses"
		} else if (maxAllowedRange.last > 1) {
			StyledButton {
				text = "Plusieurs réponses"
				action = {
					props.replace(props.field.requestCopy(
						arity = Arity.list(0, 5)
							.expandMin(minAllowedRange.last)
							.truncateMin(minAllowedRange.first)
							.expandMax(maxAllowedRange.first)
							.truncateMax(maxAllowedRange.last)
					))
				}
			}
		}
	} else {
		val message = when (arity) {
			Arity.mandatory() -> "Obligatoire"
			Arity.optional() -> "Facultatif"
			Arity.forbidden() -> "Caché"
			else -> "De ${arity.min} à ${arity.max} réponses"
		}

		StyledButton {
			text = message
			enabled = false
		}
	}
}

private fun updateSubFieldsOnMaxArityChange(
	props: EditableFieldProps,
	newArity: Arity,
	composites: List<Composite>,
) {
	val newField = props.field.requestCopy(arity = newArity)

	if (newField !is FormField.Composite) {
		props.replace(newField)
	} else {
		val composite = when (newField) {
			is ShallowFormField.Composite -> newField.ref
				.also { it.loadIfNecessary(composites) }
				.obj
			is DeepFormField.Composite -> (newField.ref.obj as DataField.Composite).ref
				.also { it.loadIfNecessary(composites) }
				.obj
			else -> error("This is impossible, the execution should never reach this point.")
		}
		val newFields = composite.fields.map { it.createMatchingFormField(composites) }

		when (newField) {
			is ShallowFormField.Composite -> props.replace(newField.copy(fields = newFields))
			is DeepFormField.Composite -> props.replace(newField.copy(fields = newFields))
		}
	}
}
