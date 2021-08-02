package formulaide.api.fields

import formulaide.api.fields.DataField.Composite
import formulaide.api.types.Arity
import formulaide.api.types.Ref
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
	 * This function is not recursive, see [fieldMonad].
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

		override fun toString() = "Data.Union($id, $name, order=$order, $arity, $options)"
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
			require(arity.min == 0) { "L'arité minimale d'une donnée composée doit être 0, pour éviter les récursions infinies" }
		}

		override fun validate() {
			super.validate()
			require(ref.loaded) { "La donnée composée $id référence ${ref.id} qui n'a pas été chargée" }
		}

		override fun toString() = "Data.Composite($id, $name, order=$order, $arity, composite=$ref)"
	}

	fun copyToSimple(simple: SimpleField) = Simple(id, order, name, simple)
	fun copyToUnion(options: List<DataField>) = Union(id, order, name, arity, options)
	fun copyToComposite(composite: Ref<CompositeData>) =
		Composite(id, order, name, arity.expandMin(0), composite)
}