package com.tylor.repwise.food

import com.tylor.repwise.core.FoodNutrition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Online food lookup.
 *  - Open Food Facts: free, no key, ~3M+ products worldwide, strongest on packaged barcodes.
 *  - USDA FoodData Central: ~400k US branded foods (barcodes) plus generic foods like "banana".
 *    Works with the shared DEMO_KEY (low rate limit); a free personal key raises it.
 */
class FoodLookup(private val usdaKey: () -> String) {

    /** Barcode -> food. Tries Open Food Facts first, then USDA. Null when neither has it. */
    suspend fun byBarcode(code: String): FoodNutrition? = withContext(Dispatchers.IO) {
        runCatching { offProduct(code) }.getOrNull() ?: runCatching { usdaBarcode(code) }.getOrNull()
    }

    /** Text search across both databases, USDA first because its generic foods are cleaner. */
    suspend fun search(query: String): List<FoodNutrition> = withContext(Dispatchers.IO) {
        coroutineScope {
            val usda = async { runCatching { usdaSearch(query) }.getOrDefault(emptyList()) }
            val off = async { runCatching { offSearch(query) }.getOrDefault(emptyList()) }
            (usda.await() + off.await()).distinctBy { (it.name + it.brand).lowercase() }
        }
    }

    // ---- Open Food Facts ----

    private val offFields = "code,product_name,brands,nutriments,serving_size,serving_quantity"

    private fun offProduct(code: String): FoodNutrition? {
        val json = get("https://world.openfoodfacts.org/api/v2/product/${enc(code)}?fields=$offFields")
        if (json.optInt("status") != 1) return null
        return offParse(json.getJSONObject("product"), code)
    }

    private fun offSearch(q: String): List<FoodNutrition> {
        val json = get("https://world.openfoodfacts.org/cgi/search.pl?search_terms=${enc(q)}&search_simple=1&json=1&page_size=20&fields=$offFields")
        val arr = json.optJSONArray("products") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { offParse(arr.getJSONObject(it), null) }
    }

    private fun offParse(p: JSONObject, code: String?): FoodNutrition? {
        val name = p.optString("product_name").trim().ifBlank { return null }
        val n = p.optJSONObject("nutriments") ?: return null
        val kcal = n.optDouble("energy-kcal_100g").takeUnless { it.isNaN() }
            ?: n.optDouble("energy_100g").takeUnless { it.isNaN() }?.div(4.184)
            ?: return null
        return FoodNutrition(
            name = name,
            brand = p.optString("brands").split(",").firstOrNull()?.trim()?.ifBlank { null },
            barcode = code ?: p.optString("code").ifBlank { null },
            kcalPer100g = kcal,
            proteinPer100g = n.optDouble("proteins_100g", 0.0),
            carbsPer100g = n.optDouble("carbohydrates_100g", 0.0),
            fatPer100g = n.optDouble("fat_100g", 0.0),
            servingGrams = p.optDouble("serving_quantity").takeUnless { it.isNaN() || it <= 0 },
            servingLabel = p.optString("serving_size").ifBlank { null },
            source = "Open Food Facts",
        )
    }

    // ---- USDA FoodData Central ----

    private fun usdaSearch(q: String, dataTypes: String = "Foundation,SR%20Legacy,Branded"): List<FoodNutrition> {
        val json = get("https://api.nal.usda.gov/fdc/v1/foods/search?api_key=${enc(usdaKey())}&query=${enc(q)}&pageSize=20&dataType=$dataTypes")
        val arr = json.optJSONArray("foods") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { usdaParse(arr.getJSONObject(it)) }
    }

    private fun usdaBarcode(code: String): FoodNutrition? {
        val want = code.trimStart('0')
        return usdaSearch(code, "Branded").firstOrNull { it.barcode?.trimStart('0') == want }
    }

    private fun usdaParse(f: JSONObject): FoodNutrition? {
        val nutrients = f.optJSONArray("foodNutrients") ?: return null
        val byNumber = (0 until nutrients.length()).associate {
            val o = nutrients.getJSONObject(it); o.optString("nutrientNumber") to o.optDouble("value", 0.0)
        }
        val kcal = byNumber["208"] ?: byNumber["957"] ?: byNumber["958"] ?: return null
        val unit = f.optString("servingSizeUnit").uppercase()
        val serving = f.optDouble("servingSize").takeIf { !it.isNaN() && it > 0 && (unit == "GRM" || unit == "G" || unit == "MLT" || unit == "ML") }
        return FoodNutrition(
            name = f.optString("description").lowercase().replaceFirstChar(Char::uppercase),
            brand = f.optString("brandName").ifBlank { f.optString("brandOwner") }.ifBlank { null },
            barcode = f.optString("gtinUpc").ifBlank { null },
            kcalPer100g = kcal,
            proteinPer100g = byNumber["203"] ?: 0.0,
            carbsPer100g = byNumber["205"] ?: 0.0,
            fatPer100g = byNumber["204"] ?: 0.0,
            servingGrams = serving,
            servingLabel = f.optString("householdServingFullText").ifBlank { null },
            source = "USDA",
        )
    }

    // ---- HTTP ----

    private fun get(url: String): JSONObject {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 15_000
        conn.setRequestProperty("User-Agent", "RepWise/0.2 (personal Android app)")
        try {
            if (conn.responseCode != 200) error("HTTP ${conn.responseCode}")
            return JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
        } finally { conn.disconnect() }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    companion object { const val DEMO_KEY = "DEMO_KEY" }
}
