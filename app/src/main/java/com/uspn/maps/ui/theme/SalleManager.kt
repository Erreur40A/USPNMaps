package com.uspn.maps.ui.theme

import android.content.Context
import androidx.appcompat.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.uspn.maps.DatabaseHelper
import com.uspn.maps.Salle

class SearchManager(
    private val context: Context,
    private val dbHelper: DatabaseHelper
) {
    private var allSalles: List<Salle> = emptyList()
    private var selectedSalle: Salle? = null
    var onSalleSelected: ((Salle) -> Unit)? = null

    init {
        allSalles = dbHelper.getAllData()
    }

    fun createSearchLayout(): LinearLayout {
        val searchLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 16, 16, 16)
            elevation = 8f
        }
        lateinit var clearButton: ImageButton
        lateinit var searchBar: AutoCompleteTextView

        ViewCompat.setOnApplyWindowInsetsListener(searchLayout) { view, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.setPadding(
                view.paddingLeft,
                statusBarInsets.top + 16,
                view.paddingRight,
                view.paddingBottom
            )
            insets
        }

        val backgroundSearchBar = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 64f
            setColor("#FFFFFF".toColorInt())
        }

        // Création de l'adapter
        val salleAdapter = SalleAdapter(context, allSalles)

        val searchBarContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f  // Prend toute la largeur disponible
            )
            setPadding(paddingLeft, paddingTop, 12, paddingBottom)
            background = backgroundSearchBar  // Le fond blanc arrondi
        }



        searchBar = AutoCompleteTextView(context).apply {
            hint = "Rechercher une salle..."
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            threshold = 1
            imeOptions = EditorInfo.IME_ACTION_DONE
            setPadding(32, paddingTop, 8, paddingBottom)
            background = null
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END

            setAdapter(salleAdapter)

            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    // Afficher/masquer le bouton clear
                    clearButton.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
                }
            })

            // Quand l'utilisateur sélectionne une salle
            setOnItemClickListener { parent, view, position, id ->
                val salle = salleAdapter.getItem(position)
                if (salle != null) {
                    selectedSalle = salle

                    // Afficher la salle dans la barre de recherche
                    setText(salle.nom, false)
                    clearFocus()
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(windowToken, 0)

                    handleSalleSelection(salle)
                } else {
                    Log.e("SearchManager", "Salle sélectionnée null à la position $position")
                }
            }
        }

        clearButton = ImageButton(context).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            visibility = View.GONE

            // ✅ Centrer verticalement avec MATCH_PARENT et gravity
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT  // Prend la hauteur du parent
            ).apply {
                gravity = Gravity.CENTER_VERTICAL  // Centre verticalement
            }

            setPadding(8, 8, 8, 8)  // Padding légèrement plus grand pour respirer

            // Fond circulaire bleu foncé plus petit
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#293358"))
                setSize(28, 28)  // Taille du cercle (28x28 au lieu de 12x12)
            }

            // Réduire la taille de l'icône
            scaleX = 0.6f
            scaleY = 0.6f

            setColorFilter(Color.WHITE)

            setOnClickListener {
                searchBar.setText("")
                selectedSalle = null
                searchBar.clearFocus()
            }
        }

        val searchButton = ImageButton(context).apply {
            setImageResource(android.R.drawable.ic_menu_search)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener {
                val query = searchBar.text.toString()
                if (query.isNotBlank()) {
                    val results = filterSalles(query)
                    showSearchResults(results)
                } else {
                    Toast.makeText(context, "Veuillez entrer un texte", Toast.LENGTH_SHORT).show()
                }
            }
        }

        searchBarContainer.addView(searchBar)
        searchBarContainer.addView(clearButton)

        searchLayout.addView(searchBarContainer)
        searchLayout.addView(searchButton)

        val backgroundSearchLayout = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor("#293358".toColorInt())
        }
        searchLayout.background = backgroundSearchLayout

        return searchLayout
    }

    // Méthode de filtrage
    fun filterSalles(query: String): List<Salle> {
        val queryLower = query.lowercase()

        return allSalles.filter { salle ->
            salle.nom.lowercase().contains(queryLower) ||
                    //salle.batiment.lowercase().contains(queryLower) ||
                    salle.description?.lowercase()?.contains(queryLower) == true
        }.sortedWith(compareBy(
            { !it.nom.lowercase().startsWith(queryLower) },
            { !it.nom.lowercase().contains(queryLower) },
            //{ !it.batiment.lowercase().contains(queryLower) },
            { !(it.description?.lowercase()?.contains(queryLower) == true) },
            { it.nom }
        ))
    }

    // Quand une salle est sélectionnée
    private fun handleSalleSelection(salle: Salle) {
        Log.d("SearchManager", "Salle sélectionnée: ${salle.toString()}")
        onSalleSelected?.invoke(salle)
        Toast.makeText(
            context,
            // TODO A REVOID BATIMENID

            "Salle: ${salle.nom}\nBâtiment: ${salle.batiment.code}",
            Toast.LENGTH_SHORT
        ).show()
    }

    // Afficher les résultats de recherche (dialogue)
    private fun showSearchResults(results: List<Salle>) {
        if (results.isEmpty()) {
            Toast.makeText(context, "Aucune salle trouvée", Toast.LENGTH_SHORT).show()
            return
        }
// TODO A REVOID BATIMENID
        val items = results.map { "${it.nom} - ${it.batiment.code}" }.toTypedArray()

        AlertDialog.Builder(context)
            .setTitle("Résultats (${results.size})")
            .setItems(items) { _, which ->
                handleSalleSelection(results[which])
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    fun getSelectedSalle(): Salle? = selectedSalle

    // Récupérer toutes les salles (utile pour d'autres utilisations)
    fun getAllSalles(): List<Salle> = allSalles
}