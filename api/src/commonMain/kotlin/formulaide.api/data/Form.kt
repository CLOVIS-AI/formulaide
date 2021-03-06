package formulaide.api.data

import formulaide.api.fields.FormRoot
import formulaide.api.types.OrderedListElement.Companion.checkOrderValidity
import formulaide.api.types.Ref
import formulaide.api.types.Referencable
import formulaide.api.users.TokenResponse
import kotlinx.serialization.Serializable

/**
 * The declaration of a form.
 *
 * This type represents the structure of a form. For a submission, see [FormSubmission].
 *
 * A form declaration is composed of 3 parts:
 * - Metadata ([name], [public]…)
 * - Fields that the user needs to fill in ([mainFields])
 * - Actions that will be taken to check that a submission is valid ([actions])
 *
 * @property name The display name of this form
 * @property public Whether this form is visible from anonymous users.
 * If `false`, only users that have logged in (that possess a valid [TokenResponse]) can see it.
 * @property mainFields The fields that the user needs to fill in when creating a [FormSubmission].
 * @property actions The steps taken to validate a [FormSubmission].
 * If the user querying this form is not allowed to see the list of actions
 * (for example, anonymous users), the list is empty.
 *
 * @see formulaide.api.dsl.form
 */
@Serializable
data class Form(
	override val id: String,
	val name: String,
	val open: Boolean,
	val public: Boolean,
	val mainFields: FormRoot,
	val actions: List<Action>,
) : Referencable {

	init {
		mainFields.fields.checkOrderValidity()
		require(name.isNotBlank()) { "Le nom d'un formulaire ne peut pas être vide : '$name'" }

		require(actions.isNotEmpty()) { "Un formulaire doit contenir au moins une action." }
	}

	/**
	 * Loads all there is to load in this [Form] (recursively).
	 *
	 * Parameters [allowNotFound] and [lazy] are passed to [Ref.loadFrom].
	 */
	fun load(composites: List<Composite>, allowNotFound: Boolean = false, lazy: Boolean = true) {
		mainFields.load(composites, allowNotFound, lazy)

		for (action in actions)
			action.fields?.load(composites, allowNotFound, lazy)
	}

	/**
	 * Validates all constraints in this form.
	 *
	 * The form should be [loaded][load] beforehand.
	 */
	fun validate() {
		mainFields.validate()
		actions.checkOrderValidity()
	}
}

//region API objects

@Serializable
data class FormMetadata(
	val form: Ref<Form>,
	val open: Boolean? = null,
	val public: Boolean? = null,
	val mainFields: FormRoot? = null,
	val actions: List<Action>? = null,
)

//endregion
