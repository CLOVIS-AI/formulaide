package formulaide.ui.screens.review

import formulaide.api.data.ParsedSubmission
import formulaide.api.data.Record
import formulaide.api.data.RecordStateTransition
import formulaide.api.fields.FormField
import formulaide.api.types.Ref.Companion.load
import formulaide.client.Client
import formulaide.client.routes.findSubmission
import formulaide.ui.components.CrashReporter
import formulaide.ui.components.cards.Card
import formulaide.ui.components.useAsync
import formulaide.ui.reportExceptions
import formulaide.ui.useClient
import formulaide.ui.useUser
import formulaide.ui.utils.DelegatedProperty.Companion.asDelegated
import formulaide.ui.utils.useEquals
import formulaide.ui.utils.useListEquality
import react.FC
import react.dom.html.ReactHTML.tr
import react.useEffect
import react.useState

internal data class ParsedTransition(
	val transition: RecordStateTransition,
	val submission: ParsedSubmission?,
)

external interface ReviewRecordProps : RecordTableProps {
	var record: Record
	var columnsToDisplay: List<Pair<String, FormField>>
}

val ReviewRecord = FC<ReviewRecordProps>("ReviewRecord") { props ->
	props.record.form.load(props.form)
	props.record.load()

	val scope = useAsync()
	val (client) = useClient()
	val user by useUser()
	require(client is Client.Authenticated) { "Cannot display a ReviewRecord for an unauthenticated user" }

	var showFullHistory by useState(false)
	fun getHistory() = props.record.history.map { ParsedTransition(it, null) }
	val (fullHistory, setFullHistory) = useState(getHistory())
		.asDelegated()
		.useEquals()
		.useListEquality()
	val history =
		if (showFullHistory) fullHistory
			.sortedBy { it.transition.timestamp }
		else fullHistory.groupBy { it.transition.previousState }
			.mapNotNull { (_, v) -> v.maxByOrNull { it.transition.timestamp } }
			.sortedBy { it.transition.timestamp }

	useEffect(props.record) {
		reportExceptions {
			val newHistory = getHistory()
			if (newHistory.map { it.transition } != fullHistory.map { it.transition }) {
				setFullHistory { newHistory }
			}
		}
	}

	useEffect(fullHistory, showFullHistory) {
		scope.reportExceptions {
			val edits = HashMap<Int, ParsedTransition>()

			for ((i, parsed) in fullHistory.withIndex()) {
				val fields = parsed.transition.fields
				if (fields != null && parsed.submission == null) {
					fields.load { client.findSubmission(it) }
					val newParsed = parsed.copy(submission = fields.obj.parse(props.form))
					edits[i] = newParsed
				}
			}

			setFullHistory {
				val result = ArrayList<ParsedTransition>()
				for (i in this.indices) {
					result.add(edits[i] ?: this[i])
				}
				result
			}
		}
	}

	if (user == null) {
		Card {
			title = "Dossier"
			loading = true
			+"Chargement de l'utilisateur???"
		}
		return@FC
	}

	tr {
		CrashReporter {
			if (props.expandedRecords[props.record] == false) {
				ReviewRecordCollapsed {
					+props
					this.history = history
					this.showFullHistory = showFullHistory
				}
			} else {
				ReviewRecordExpanded {
					+props
					this.history = history
					this.showFullHistory = showFullHistory
					this.updateShowFullHistory = { showFullHistory = it }
				}
			}
		}
	}
}
