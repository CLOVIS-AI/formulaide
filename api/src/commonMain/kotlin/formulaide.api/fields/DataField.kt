package formulaide.api.fields

import formulaide.api.fields.DataField.Composite
import formulaide.api.types.Arity
import formulaide.api.types.OrderedListElement.Companion.checkOrderValidity
import formulaide.api.types.Ref
import formulaide.api.types.Ref.Companion.ids
import formulaide.api.types.expandMin
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import formulaide.api.data.Composite as CompositeData

/**
 * A field in a [composite data structure][CompositeData].
 *
 * Data fields do not recurse on [Composite], instead they simply refer to another composite data structure (implicit recursion).
 */
@Serializable
sealed class DataField : Field {

	/**
	 * Checks that all constraints of this [DataField] are respected.
	 *
	 * This function is not recursive, see [asSequence].
	 */
	open fun validate() = Unit

	/**
	 * A field that represents a single data entry.
	 *
	 * For more information, see [Field.Simple].
	 */
	@Serializable
	@SerialName("DATA_SIMPLE")
	data class Simple(
		override val id: String,
		override val order: Int,
		override val name: String,
		override val simple: SimpleField,
	) : DataField(), Field.Simple {

		override fun toString() = "Data.Simple($id, $name, order=$order, $simple)"
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
	@SerialName("DATA_UNION")
	data class Union(
		override val id: String,
		override val order: Int,
		override val name: String,
		override val arity: Arity,
		override val options: List<DataField>,
	) : DataField(), Field.Union<DataField> {

		override fun validate() {
			super.validate()
			options.checkOrderValidity()
			require(options.ids() == options.ids()
				.distinct()) { "Plusieurs champs de cette union ont le m??me identifiant : ${options.ids()}" }
		}

		override fun toString() = "Data.Union($id, $name, order=$order, $arity, $options)"
		override fun requestCopy(name: String?, arity: Arity?, order: Int?) = copy(
			name = name ?: this.name,
			arity = arity ?: this.arity,
			order = order ?: this.order,
		)
	}

	/**
	 * A field that represents another composite data structure.
	 *
	 * Composite data structures only [reference][ref] other composite data structures, and cannot
	 * override their settings (unlike [forms][ShallowFormField.Composite]).
	 *
	 * Because there is no recursion here, this class doesn't implement [Field.Container].
	 *
	 * @property ref The [Composite][CompositeData] this object represents.
	 */
	@Serializable
	@SerialName("DATA_REFERENCE")
	data class Composite(
		override val id: String,
		override val order: Int,
		override val name: String,
		override val arity: Arity,
		override val ref: Ref<CompositeData>,
	) : DataField(), Field.Reference<CompositeData> {

		init {
			require(arity.min == 0) { "L'arit?? minimale d'une donn??e compos??e doit ??tre 0, pour ??viter les r??cursions infinies" }
		}

		override fun validate() {
			super.validate()
			require(ref.loaded) { "La donn??e compos??e $id r??f??rence ${ref.id} qui n'a pas ??t?? charg??e" }
		}

		override fun toString() = "Data.Composite($id, $name, order=$order, $arity, composite=$ref)"
		override fun requestCopy(name: String?, arity: Arity?, order: Int?) = copy(
			name = name ?: this.name,
			arity = arity ?: this.arity,
			order = order ?: this.order,
		)
	}

	abstract override fun requestCopy(name: String?, arity: Arity?, order: Int?): DataField

	fun copyToSimple(simple: SimpleField): Simple = Simple(id, order, name, simple)
	fun copyToUnion(options: List<DataField>) = Union(id, order, name, arity, options)
	fun copyToComposite(composite: Ref<CompositeData>) =
		Composite(id, order, name, arity.expandMin(0), composite)
}
