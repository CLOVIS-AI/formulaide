package formulaide.server.routes

import formulaide.api.users.ServiceModification
import formulaide.db.document.*
import formulaide.server.Auth.Companion.Employee
import formulaide.server.Auth.Companion.requireAdmin
import formulaide.server.Auth.Companion.requireEmployee
import formulaide.server.database
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*

fun Routing.serviceRoutes() {
	route("/services") {

		authenticate(Employee) {
			get("/list") {
				call.requireEmployee(database)

				val services = database.allServices()
					.map(DbService::toApi)

				call.respond(services)
			}

			get("/fullList") {
				call.requireAdmin(database)

				val services = database.allServicesIgnoreOpen()
					.map(DbService::toApi)

				call.respond(services)
			}

			post("/create") {
				call.requireAdmin(database)

				val service = call.receive<String>()
				val created = database.createService(service)

				call.respond(created)
			}

			post("/close") {
				call.requireAdmin(database)

				val service = call.receive<ServiceModification>()
				database.manageService(service.id, service.open)

				call.respond(
					database.findService(service.id)
						?: error("Le service est introuvable alors qu'il a déjà été modifié, c'est impossible")
				)
			}
		}

	}

}
