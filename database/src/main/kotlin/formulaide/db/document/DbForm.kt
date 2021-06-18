package formulaide.db.document

import formulaide.api.data.Data
import formulaide.api.data.Form
import formulaide.api.data.FormId
import formulaide.db.Database
import org.litote.kmongo.eq
import org.litote.kmongo.match
import kotlin.random.Random

/**
 * Gets the list of forms.
 *
 * @param public If `true`, searches for public forms. If `false`, searches for internal forms.
 * If `null`, searches for all forms without regards for their visibility. See [Form.public].
 * @param open If `true`, searches for open forms. If `false`, searches for closed.
 * If `null`, searches for all forms without regards for their status. See [Form.open].
 */
suspend fun Database.listForms(public: Boolean?, open: Boolean? = true): List<Form> {
	val matchPublic =
		if (public != null) match(Form::public eq public)
		else null

	val matchOpen =
		if (open != null) match(Form::open eq open)
		else null

	val pipeline = arrayOf(matchPublic, matchOpen)
		.filterNotNull()

	return forms.aggregate<Form>(pipeline).toList()
}

suspend fun Database.findForm(id: FormId) =
	forms.findOne(Form::id eq id)

suspend fun Database.createForm(form: Form): Form {
	require(form.open) { "Il est interdit de créer un formulaire fermé" }

	form.fields.forEach { field ->
		val data = field.data
		if (data is Data.Compound)
			require(findData(data.id) != null) { "Le champ ${field.name} fait référence à une donnée qui n'existe pas : $data" }
	}

	var id: Int
	do {
		id = Random.nextInt()
	} while (findForm(id) != null)

	val newForm = form.copy(id = id)
	forms.insertOne(newForm)
	return newForm
}