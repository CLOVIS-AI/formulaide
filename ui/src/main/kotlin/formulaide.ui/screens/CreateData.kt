package formulaide.ui.screens

import formulaide.api.data.Composite
import formulaide.api.fields.DataField
import formulaide.api.fields.Field
import formulaide.api.fields.SimpleField
import formulaide.api.types.Arity
import formulaide.api.types.Ref
import formulaide.client.Client
import formulaide.client.routes.createData
import formulaide.ui.*
import formulaide.ui.components.*
import formulaide.ui.fields.FieldEditor
import formulaide.ui.fields.SwitchDirection
import formulaide.ui.utils.remove
import formulaide.ui.utils.replace
import formulaide.ui.utils.text
import kotlinx.html.InputType
import org.w3c.dom.HTMLInputElement
import react.*

val CreateData = fc<RProps> { _ ->
	traceRenders("CreateData")

	val (client) = useClient()
	if (client !is Client.Authenticated) {
		styledCard("Créer un groupe",
		           failed = true) { text("Seuls les administrateurs peuvent créer un groupe.") }
		return@fc
	}

	val formName = useRef<HTMLInputElement>()
	val (fields, setFields) = useState<List<DataField>>(emptyList())
	val (_, navigateTo) = useNavigation()
	var maxId by useState(0)

	val lambdas = useLambdas()

	styledFormCard(
		"Créer un groupe",
		"Grouper des données utilisées dans plusieurs formulaires permet de mieux gérer les mises à jours. " +
				"Les données composées sont stockées et unifiées entre les services.",
		"Créer ce groupe" to {
			val data = Composite(
				id = Ref.SPECIAL_TOKEN_NEW,
				name = formName.current?.value ?: error("Ce groupe n'a pas de nom"),
				fields = fields
			)

			launch {
				client.createData(data)
				refreshComposites()
				navigateTo(Screen.ShowData)
			}
		},
	) {
		styledField("new-data-name", "Nom") {
			styledInput(InputType.text, "new-data-name", required = true, ref = formName) {
				autoFocus = true
			}
		}

		styledField("data-fields", "Champs") {
			for ((i, field) in fields.withIndex()) {
				child(FieldEditor) {
					attrs {
						this.field = field
						key = field.id
						replace = { it: Field ->
							setFields { fields -> fields.replace(i, it as DataField) }
						}.memoIn(lambdas, "replace-${field.id}", i)
						remove = {
							setFields { fields -> fields.remove(i) }
						}.memoIn(lambdas, "remove-${field.id}", i)
						switch = { direction: SwitchDirection ->
							setFields { fields ->
								val otherIndex = i + direction.offset
								val other = fields.getOrNull(otherIndex)

								if (other != null) {
									fields
										.replace(i, field.requestCopy(order = other.order))
										.replace(otherIndex, other.requestCopy(order = field.order))
										.sortedBy { it.order }
								} else fields
							}
						}.memoIn(lambdas, "switch-${field.id}", i)

						depth = 0
						fieldNumber = i
					}
				}
			}

			styledButton("Ajouter un champ", action = {
				setFields { fields ->
					fields + DataField.Simple(
						order = fields.size,
						id = (maxId++).toString(),
						name = "Nouveau champ",
						simple = SimpleField.Text(Arity.optional())
					)
				}
			})
		}
	}
}
