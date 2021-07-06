package formulaide.ui

import formulaide.api.data.Composite
import formulaide.api.data.Form
import formulaide.api.users.Service
import formulaide.api.users.User
import formulaide.client.Client
import formulaide.client.routes.*
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import react.*

internal val defaultClient =
	Client.Anonymous.connect(window.location.protocol + "//" + window.location.host)
private val defaultClientTest = Client.Anonymous.connect("http://localhost:8000")

/**
 * The main app screen.
 */
val App = functionalComponent<RProps> {
	val (client, setClient) = useState<Client>(defaultClient)
	val (user, setUser) = useState<User?>(null)
	val (scope, _) = useState(MainScope())

	//region Refresh the user if necessary
	useEffect(client) {
		scope.launch {
			if (client is Client.Authenticated)
				setUser(client.getMe())
		}
	}
	//endregion

	//region Global list of compounds
	val (composites, setComposites) = useState<List<Composite>>(emptyList())
	val (refreshComposites, setRefreshComposites) = useState(0)
	useEffect(client, user, refreshComposites) {
		if (client is Client.Authenticated) {
			scope.launch { setComposites(client.listData()) }
		} else setComposites(emptyList())
	}
	//endregion

	//region Global list of forms
	val (forms, setForms) = useState<List<Form>>(emptyList())
	val (refreshForms, setRefreshForms) = useState(0)
	useEffect(client, user, refreshForms) {
		scope.launch {
			setForms(
				if (client is Client.Authenticated) client.listAllForms()
				else
					try {
						client.listForms()
					} catch (e: Exception) {
						console.warn("Couldn't access the list of forms, this client is probably dead. Switching to the test client.")
						setClient(defaultClientTest)
						setUser(null)
						emptyList()
					}
			)
		}
	}
	//endregion

	//region Global list of services (only if admin)
	val (services, setServices) = useState(emptyList<Service>())
	val (refreshServices, setRefreshServices) = useState(0)
	useEffect(client, user, refreshServices) {
		if (client is Client.Authenticated) {
			scope.launch {
				setServices(
					if (user!!.administrator) client.listAllServices()
					else client.listServices()
				)
			}
		} else setServices(emptyList())
	}
	//endregion

	child(Window) {
		attrs {
			this.client = client
			this.user = user
			this.connect = { setClient(it); setUser(null) }

			this.scope = scope

			this.composites = composites
			this.refreshComposites = { setRefreshComposites(refreshComposites + 1) }

			this.forms = forms
			this.refreshForms = { setRefreshForms(refreshForms + 1) }

			this.services = services
			this.refreshServices = { setRefreshServices(refreshServices + 1) }
		}
	}

}
