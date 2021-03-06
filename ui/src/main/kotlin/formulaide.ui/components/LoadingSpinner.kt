package formulaide.ui.components

import formulaide.ui.utils.classes
import react.FC
import react.Props
import react.dom.svg.ReactSVG.circle
import react.dom.svg.ReactSVG.path
import react.dom.svg.ReactSVG.svg

val LoadingSpinner = FC<Props>("LoadingSpinner") {
	svg {
		classes = "animate-spin h-4 w-4 mx-2 inline"
		viewBox = "0 0 24 24"

		circle {
			classes = "opacity-25"
			cx = 12.0
			cy = 12.0
			r = 10.0
			stroke = "currentColor"
			strokeWidth = 4.0
		}

		path {
			classes = "opacity-100"
			fill = "currentColor"
			d = "M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 " +
					"7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
		}
	}
}
