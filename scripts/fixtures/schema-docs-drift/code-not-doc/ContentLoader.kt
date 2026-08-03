private fun parseFixture(fields: Map<String, String>, errors: MutableList<String>): String? =
    fields.required("file", "fixture", "codeOnly", errors)
