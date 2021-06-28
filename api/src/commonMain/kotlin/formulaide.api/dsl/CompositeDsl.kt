package formulaide.api.dsl

import formulaide.api.data.Composite
import formulaide.api.data.SPECIAL_TOKEN_RECURSION
import formulaide.api.fields.DataField
import formulaide.api.fields.SimpleField
import formulaide.api.types.Arity
import formulaide.api.types.Ref
import formulaide.api.types.Ref.Companion.createRef

@ApiDsl
class CompositeDsl<F: DataField>(
	private var lastId: Int = 0,
	internal val fields: MutableList<F> = ArrayList(5),
) {

	internal fun nextInfo(): Pair<String, Int> = lastId++
		.let { it.toString() to it }
}

/**
 * DSL builder to instantiate a [Composite].
 */
fun composite(
	name: String,
	fields: CompositeDsl<DataField>.() -> Unit
) : Composite {
	val dsl = CompositeDsl<DataField>()
	dsl.fields()

	return Composite(
		Ref.SPECIAL_TOKEN_NEW,
		name,
		dsl.fields
	)
}

fun CompositeDsl<DataField>.simple(
	name: String,
	field: SimpleField
): DataField.Simple {
	val (id, order) = nextInfo()

	return DataField.Simple(id, order, name, field).also { fields += it }
}

fun CompositeDsl<DataField>.union(
	name: String,
	arity: Arity,
	options: CompositeDsl<DataField.Union>.() -> Unit
) : DataField.Union {
	val dsl = CompositeDsl<DataField.Union>()
	dsl.options()

	val (id, order) = nextInfo()

	return DataField.Union(
		id,
		order,
		name,
		arity,
		dsl.fields
	).also { fields += it }
}

fun CompositeDsl<DataField>.composite(
	name: String,
	arity: Arity,
	composite: Ref<Composite>
) : DataField.Composite {
	val (id, order) = nextInfo()

	return DataField.Composite(id, order, name, arity, composite).also { fields += it }
}

fun CompositeDsl<DataField>.composite(
	name: String,
	arity: Arity,
	composite: Composite
) = this.composite(name, arity, composite.createRef())

fun CompositeDsl<DataField>.compositeItself(
	name: String,
	arity: Arity
) = this.composite(name, arity, Ref(SPECIAL_TOKEN_RECURSION))
