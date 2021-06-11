package formulaide.api.data

import formulaide.api.data.OrderedListElement.Companion.checkOrderValidity
import formulaide.api.users.TokenResponse
import kotlinx.serialization.Serializable

/**
 * Id of [Form].
 */
typealias FormId = Int

/**
 * The declaration of a form.
 *
 * This type represents the structure of a form. For a submission, see [FormSubmission].
 *
 * A form declaration is composed of 3 parts:
 * - Metadata ([name], [public]…)
 * - Fields that the user needs to fill in ([fields])
 * - Actions that will be taken to check that a submission is valid ([actions])
 *
 * @property name The display name of this form
 * @property public Whether this form is visible from anonymous users.
 * If `false`, only users that have logged in (that possess a valid [TokenResponse]) can see it.
 * @property fields The fields that the user needs to fill in when creating a [FormSubmission].
 * @property actions The steps taken to validate a [FormSubmission].
 * If the user querying this form is not allowed to see the list of actions
 * (for example, anonymous users), the list is empty.
 */
@Serializable
data class Form(
	val name: String,
	val id: FormId,
	val public: Boolean,
	val fields: List<FormField>,
	val actions: List<Action>,
) {
	init {
		fields.checkOrderValidity()
		require(name.isNotBlank()) { "Le nom d'un formulaire ne peut pas être vide : '$name'" }
	}
}

/**
 * ID of [AbstractFormField].
 *
 * Is guaranteed unique only among children of the same object ([Form] for [FormField], [FormField] for [FormFieldComponent]).
 */
typealias FormFieldId = Int

/**
 * Abstraction over fields in a [Form].
 *
 * A distinction is made between [top-level fields][FormField] and [non-top-level fields][FormFieldComponent].
 * See their documentation for the rationale.
 */
sealed interface AbstractFormField {
	val components: List<FormFieldComponent>?
	val id: Int
}

/**
 * A top-level field in a [Form].
 *
 * @property id The ID of this field in a specific [Form]. The ID is not globally unique.
 * @property type The type of this field.
 * @property components The different components of this field, if it is a [DataId.COMPOUND] type.
 * `null` in any other case (`type == COMPOUND <=> components != null`).
 * @property name The display name of this field.
 * When the type is a compound, this property should be used instead of [CompoundData.name].
 * This is because the same compound type can be used multiple times in a form.
 */
@Serializable
data class FormField(
	override val id: FormFieldId,
	override val components: List<FormFieldComponent>? = null,
	override val minArity: Int,
	override val maxArity: Int,
	override val order: Int,
	val name: String,
	val type: Data,
) : DataList, AbstractFormField, OrderedListElement {
	init {
		checkArityValidity()
		require(name.isNotBlank()) { "Le nom d'un champ d'un formulaire ne doit être vide." }

		if (type.type == DataId.COMPOUND && maxArity != 0) {
			requireNotNull(components) { "Si ce champ représente une donnée composée, et n'est pas interdit (arité maximale de 0), alors il doit déclarer des sous-champs (components)" }
		}
	}
}

/**
 * A non-top-level field in a [Form].
 *
 * Unlike [top-level][FormField] fields, a non-top-level field does not declare
 * a type or a name. This is because non-top-level fields are necessarily part of a [CompoundData],
 * which has both ([CompoundData.name], [CompoundData.id]). The [top-level][FormField] field
 * needs to be able to override the name because the same [CompoundData] can appear multiple times,
 * however no ambiguity exists for non-top-level fields.
 *
 * @property id The ID of this field in a specific [FormField]. The ID is not globally unique.
 */
@Serializable
data class FormFieldComponent internal constructor(
	override val minArity: Int,
	override val maxArity: Int,
	override val id: CompoundDataFieldId,
	override val components: List<FormFieldComponent>? = null,
) : DataList, AbstractFormField {
	init {
		checkArityValidity()
	}

	/**
	 * Create a [FormFieldComponent] that corresponds to a [CompoundData].
	 */
	constructor(minArity: Int, maxArity: Int, compound: CompoundDataField, components: List<FormFieldComponent>) : this(minArity, maxArity, compound.id, components) {
		if (maxArity == 0)
			require(compound.type.type == DataId.COMPOUND) { "Un champ de formulaire non-interdit qui référence des sous-champs doit être de type ${DataId.COMPOUND}" }
	}

	/**
	 * Create a [FormFieldComponent] that corresponds to a [simple data][DataId] or a [union data][DataId.UNION].
	 */
	constructor(minArity: Int, maxArity: Int, compound: CompoundDataField) : this(minArity, maxArity, compound.id, null) {
		require(compound.type.type != DataId.COMPOUND) { "Un champ de formulaire qui ne référence pas de sous-champs ne doit être de type ${DataId.COMPOUND}" }
	}
}
