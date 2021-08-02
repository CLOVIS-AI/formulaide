package formulaide.ui.screens

import formulaide.api.data.*
import formulaide.api.types.Ref.Companion.createRef
import formulaide.api.types.Ref.Companion.load
import formulaide.client.Client
import formulaide.client.routes.compositesReferencedIn
import formulaide.client.routes.findSubmission
import formulaide.client.routes.review
import formulaide.client.routes.todoListFor
import formulaide.ui.components.*
import formulaide.ui.fields.field
import formulaide.ui.fields.immutableFields
import formulaide.ui.reportExceptions
import formulaide.ui.traceRenders
import formulaide.ui.useClient
import formulaide.ui.useUser
import formulaide.ui.utils.parseHtmlForm
import formulaide.ui.utils.replace
import formulaide.ui.utils.text
import react.*
import react.dom.p
import kotlin.js.Date

internal fun RecordState.displayName() = when (this) {
	is RecordState.Action -> this.current.obj.name
	is RecordState.Done -> "Acceptés"
	is RecordState.Refused -> "Refusés"
}

private data class ReviewSearch(
	val action: Action?,
	val enabled: Boolean,
	val adaptedForm: Form?,
	val searchQuery: FormSubmission?,
)

@Suppress("FunctionName")
internal fun Review(form: Form, state: RecordState, initialRecords: List<Record>) = fc<RProps> {
	traceRenders("Review ${form.name}")

	val (client) = useClient()
	require(client is Client.Authenticated) { "Seuls les employés peuvent accéder à cette page." }

	var records by useState(initialRecords)
	var searches by useState(
		listOf(ReviewSearch(null, false, null, null)) +
				form.actions
					.filter { it.fields != null }
					.map { ReviewSearch(it, false, null, null) }
	)

	val refresh: suspend () -> Unit = { records = client.todoListFor(form, state) }

	styledCard(
		"Dossiers ${state.displayName()}",
		form.name,
		"Actualiser" to refresh,
	) {
		p { text("${records.size} dossiers sont chargés. Pour des raisons de performance, il n'est pas possible de charger plus de ${Record.MAXIMUM_NUMBER_OF_RECORDS_PER_ACTION} dossiers à la fois.") }

		for ((i, search) in searches.withIndex()) {
			styledNesting(depth = 0, fieldNumber = i) {
				if (search.enabled) {
					text("Recherche en cours…")

					styledButton("Annuler la recherche",
					             action = {
						             searches = searches.replace(i, search.copy(enabled = false))
					             })
				} else {
					val message = "Recherche : " +
							(if (search.action == null) "champs originaux" else "étape ${search.action.id}") //TODO replace with action name

					styledButton(
						message,
						action = { searches = searches.replace(i, search.copy(enabled = true)) }
					)
				}
			}
		}
	}

	for (record in records) {
		child(ReviewRecord) {
			attrs {
				this.form = form
				this.record = record

				this.refresh = refresh
			}
		}
	}

}

private external interface ReviewRecordProps : RProps {
	var form: Form
	var record: Record

	var refresh: suspend () -> Unit
}

private val ReviewRecord = fc<ReviewRecordProps> { props ->
	val form = props.form
	val record = props.record
	val scope = useAsync()
	val (client) = useClient()
	val (user) = useUser()

	record.form.load(form)
	require(client is Client.Authenticated) { "Seuls les employés peuvent accéder à cette page" }

	var parsedSubmissions by useState(emptyMap<FormSubmission, ParsedSubmission>())
	useEffect(record) {
		for (submission in record.submissions)
			scope.reportExceptions {
				submission.load { client.findSubmission(it) }
				parsedSubmissions =
					parsedSubmissions.plus(submission.obj to submission.obj.parse(form))
			}
	}

	var formLoaded by useState(false)
	useEffect(form) {
		scope.reportExceptions {
			form.load(client.compositesReferencedIn(form))
			formLoaded = true
		}
	}

	val state = record.state
	val actionOrNull = (state as? RecordState.Action)?.current
	val nextState = form.actions.indexOfFirst { actionOrNull?.id == it.id }
		.takeUnless { it == -1 }
		?.let { form.actions.getOrNull(it + 1) }
		?.let { RecordState.Action(it.createRef()) }
		?: RecordState.Done

	val submitButtonText =
		if (state == RecordState.Done || state == RecordState.Refused) "Enregistrer"
		else "Accepter"

	if (user == null) {
		styledCard("Dossier", loading = true) { text("Chargement de l'utilisateur…") }
		return@fc
	}

	styledFormCard(
		"Dossier",
		null,
		submit = submitButtonText to { htmlForm ->
			val submission = if (state is RecordState.Action)
				parseHtmlForm(
					htmlForm,
					form,
					state.current.obj,
				)
			else null

			launch {
				client.review(ReviewRequest(
					record.createRef(),
					RecordStateTransition(
						(Date.now() / 1000).toLong(),
						state,
						nextState,
						assignee = user.createRef(),
						reason = null,
					),
					submission,
				))

				props.refresh()
			}
		},
		"Refuser" to {
			client.review(ReviewRequest(
				record.createRef(),
				RecordStateTransition(
					(Date.now() / 1000).toLong(),
					state,
					RecordState.Refused,
					assignee = user.createRef(),
					reason = "NOT YET IMPLEMENTED", //TODO add a reason to the review process
				),
				fields = null
			))

			props.refresh()
		},
	) {
		var i = 0

		for (submission in record.submissions) {
			styledNesting(depth = 0, fieldNumber = i) {
				if (submission.loaded && parsedSubmissions[submission.obj] != null && formLoaded) {
					immutableFields(parsedSubmissions[submission.obj]!!)
				} else {
					p { text("Chargement…"); loadingSpinner() }
				}
			}
			i++
		}

		if (state is RecordState.Action) {
			styledNesting(depth = 0, fieldNumber = i) {
				state.current.loadFrom(form.actions, lazy = true)
				val action = state.current.obj

				val root = action.fields
				if (root != null) {
					for (field in root.fields) {
						field(field)
					}
				}
				i++
			}
		}
	}
}
