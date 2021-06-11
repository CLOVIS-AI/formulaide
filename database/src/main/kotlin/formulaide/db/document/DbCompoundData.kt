package formulaide.db.document

import formulaide.api.data.*
import formulaide.db.Database
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.eq
import org.litote.kmongo.id.IdGenerator

suspend fun Database.listData() =
	data.find().toList()

suspend fun Database.createData(compound: NewCompoundData): CompoundData {
	compound.checkValidity(data)

	val id = IdGenerator.defaultGenerator.generateNewId<CompoundData>().toString()

	// 1. Create the data
	val created = CompoundData(compound.name, id, emptyList())
	data.insertOne(created)

	// 2. Remove the recursive token
	val recursiveFields = compound.fields.map { it.cleanUpRecursionToken(created) }

	// 3. Add the fields
	val valid = CompoundData(compound.name, id = id, recursiveFields)
	data.updateOne(CompoundData::id eq id, valid)

	return valid
}

private fun CompoundDataField.cleanUpRecursionToken(compound: CompoundData): CompoundDataField {
	return if (type.type == DataId.COMPOUND && type.compoundId == SPECIAL_TOKEN_RECURSION)
		copy(type = Data.compound(compound))
	else this
}

private suspend fun NewCompoundData.checkValidity(data: CoroutineCollection<CompoundData>) {
	require(fields.isNotEmpty()) { "Il est interdit de créer une donnée vide" }

	for (field in fields) {
		if (field.type.type == DataId.COMPOUND) {
			requireNotNull(field.type.compoundId) { "Si la donnée d'un champ est de type COMPOUND, il doit contenir 'compoundId'" }
			if (field.type.compoundId != SPECIAL_TOKEN_RECURSION)
				requireNotNull(data.findOne(CompoundData::id eq field.type.compoundId)) { "L'ID ${field.type.compoundId} ne correspond à aucune donnée existante" }
		} else if (field.type.type == DataId.UNION) {
			requireNotNull(field.type.union) { "Si la donnée d'un champ est de type UNION, il doit contenir 'union'" }
		}

		require(field.minArity <= field.maxArity) { "L'arité minimale doit être inférieure ou égale à l'arité supérieure (test échoué : ${field.minArity <= field.maxArity})" }
	}

	val ids = fields.map { it.id }
	val orders = fields.map { it.order }

	require(ids.distinct() == ids) { "Les identifiants utilisés pour les CompoundDataField doivent être différents les uns des autres" }
	require(orders.distinct() == orders) { "Les ordres utilisés pour les CompoundDataField doivent être différents les uns des autres" }
}
