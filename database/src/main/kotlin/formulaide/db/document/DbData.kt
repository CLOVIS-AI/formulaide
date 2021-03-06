package formulaide.db.document

import formulaide.api.data.Composite
import formulaide.api.data.SPECIAL_TOKEN_RECURSION
import formulaide.api.fields.DataField
import formulaide.api.types.Ref.Companion.createRef
import formulaide.api.types.ReferenceId
import formulaide.db.Database
import formulaide.db.utils.generateId
import org.litote.kmongo.eq

suspend fun Database.listComposites() =
	data.find().toList()

suspend fun Database.findComposite(id: ReferenceId) =
	data.findOne(Composite::id eq id)

suspend fun Database.createComposite(composite: Composite): Composite {
	composite.loadAllowRecursive(listComposites())
	composite.validate()

	val id = generateId<Composite>()

	// 1. Create the data
	val created = composite.copy(id = id)
	data.insertOne(created)

	// 2. Remove the recursive token
	val recursiveFields = composite.fields.map { it.cleanUpRecursionToken(created) }

	// 3. Add the fields
	val valid = created.copy(fields = recursiveFields)
	data.updateOne(Composite::id eq id, valid)

	return valid
}

private fun DataField.cleanUpRecursionToken(comp: Composite): DataField = when {
	this is DataField.Composite && ref.id == SPECIAL_TOKEN_RECURSION -> copy(
		ref = comp.createRef()
	)
	this is DataField.Union -> copy(
		options = options.map { it.cleanUpRecursionToken(comp) }
	)
	else -> this
}

suspend fun Database.editComposite(id: ReferenceId, open: Boolean? = null) {
	var composite = findComposite(id) ?: error("La donnée est introuvable : $id")

	if (open != null)
		composite = composite.copy(open = open)

	data.updateOne(Composite::id eq id, composite)
}
