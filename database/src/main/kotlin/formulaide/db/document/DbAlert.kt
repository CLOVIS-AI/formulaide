package formulaide.db.document

import formulaide.api.data.Alert
import formulaide.db.Database

suspend fun Database.getAlerts(): List<Alert> =
	alerts.find().toList()
