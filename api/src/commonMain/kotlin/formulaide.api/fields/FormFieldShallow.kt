package formulaide.api.fields

import formulaide.api.fields.ShallowFormField.Composite
import formulaide.api.types.Arity
import formulaide.api.types.OrderedListElement.Companion.checkOrderValidity
import formulaide.api.types.Ref
import formulaide.api.types.Ref.Companion.ids
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import formulaide.api.data.Composite as CompositeData

/**
 * Fields that do not have a transitive [Composite] parent.
 *
 * @see DeepFormField
 */
@Serializable
sealed class ShallowFormField : FormField {

	override fun load(composites: List<CompositeData>, allowNotFound: Boolean, lazy: Boolean) {}
	override fun validate() {}

	/**
	 * A field that represents a single data entry.
	 *
	 * For more information, see [Field.Simple].
	 */
	@Serializable
	@SerialName("FORM_SIMPLE_SHALLOW")
	data class Simple(
		override val id: String,
		override val order: Int,
		override val name: String,
		override val simple: SimpleField,
	) : ShallowFormField(), FormField.Simple {

		override fun toString() = "Shallow.Simple($id, $name, order=$order, $simple)"
		override fun requestCopy(name: String?, arity: Arity?, order: Int?) = copy(
			name = name ?: this.name,
			simple = simple.requestCopy(arity),
			order = order ?: this.order,
		)

		override fun requestCopy(simple: SimpleField) = copy(simple = simple)
	}

	/**
	 * A field that allows the user to choose between multiple [options].
	 *
	 * For more information, see [Field.Union].
	 */
	@Serializable
	@SerialName("FORM_UNION_SHALLOW")
	data class Union(
		override val id: String,
		override val order: Int,
		override val name: String,
		override val arity: Arity,
		override val options: List<ShallowFormField>,
	) : ShallowFormField(), FormField.Union<ShallowFormField> {

		override fun load(composites: List<CompositeData>, allowNotFound: Boolean, lazy: Boolean) {
			super.load(composites, allowNotFound, lazy)
			options.forEach { it.load(composites, allowNotFound, lazy) }
		}

		override fun validate() {
			super.validate()
			options.checkOrderValidity()
			require(options.ids() == options.ids()
				.distinct()) { "Plusieurs champs de cette union ont le m??me identifiant : ${options.ids()}" }
		}

		override fun toString() = "Shallow.Union($id, $name, order=$order, $arity, $options)"
		override fun requestCopy(name: String?, arity: Arity?, order: Int?) = copy(
			name = name ?: this.name,
			arity = arity ?: this.arity,
			order = order ?: this.order,
		)
	}

	/**
	 * A field that corresponds to a [composite data structure][CompositeData].
	 *
	 * All of its children must reference the corresponding data structure as well: see [DeepFormField].
	 */
	@Serializable
	@SerialName("FORM_COMPOSITE_SHALLOW")
	data class Composite(
		override val id: String,
		override val order: Int,
		override val name: String,
		override val arity: Arity,
		override val ref: Ref<CompositeData>,
		override val fields: List<DeepFormField>,
	) : ShallowFormField(), Field.Reference<CompositeData>, FormField.Composite {

		override fun load(composites: List<CompositeData>, allowNotFound: Boolean, lazy: Boolean) {
			super.load(composites, allowNotFound, lazy)
			ref.loadFrom(composites, allowNotFound, lazy)

			fields.forEach { it.loadRef(ref.obj.fields, allowNotFound, lazy) }
		}

		override fun validate() {
			super.validate()
			fields.checkOrderValidity()
			require(fields.ids() == fields.ids()
				.distinct()) { "Plusieurs champs de ce groupe ont le m??me identifiant : ${fields.ids()}" }
		}

		override fun toString() =
			"Shallow.Composite($id, $name, order=$order, $arity, composite=$ref, $fields)"

		override fun requestCopy(name: String?, arity: Arity?, order: Int?) = copy(
			name = name ?: this.name,
			arity = arity ?: this.arity,
			order = order ?: this.order,
		)
	}

	abstract override fun requestCopy(name: String?, arity: Arity?, order: Int?): ShallowFormField

	fun copyToSimple(simple: SimpleField): Simple = Simple(id, order, name, simple)
	fun copyToUnion(options: List<ShallowFormField>) = Union(id, order, name, arity, options)
	fun copyToComposite(
		composite: Ref<CompositeData>,
		fields: List<DeepFormField>,
	) = Composite(id, order, name, arity, composite, fields)
}
