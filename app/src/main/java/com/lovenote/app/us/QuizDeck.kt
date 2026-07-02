package com.lovenote.app.us

data class QuizQuestion(val prompt: String, val options: List<String>)

/**
 * "How well do you know each other?" deck. Each partner answers for
 * themselves and guesses the other's pick; the reveal shows who was right.
 */
object QuizDeck {
    val ALL = listOf(
        QuizQuestion("Perfect vacation?", listOf("Beach 🏖", "Mountains 🏔", "City lights 🌆", "Cozy home 🏠")),
        QuizQuestion("Movie night pick?", listOf("Romance 💕", "Action 💥", "Horror 👻", "Comedy 😂")),
        QuizQuestion("Ideal breakfast?", listOf("Big & savory 🍳", "Sweet 🥞", "Just coffee ☕", "Sleep instead 😴")),
        QuizQuestion("Dream pet?", listOf("Dog 🐶", "Cat 🐱", "Bird 🐦", "No pets 🙅")),
        QuizQuestion("Rainy day plan?", listOf("Movies in bed 🎬", "Cooking together 🍲", "Board games 🎲", "Long nap 💤")),
        QuizQuestion("Superpower of choice?", listOf("Fly ✈", "Read minds 🧠", "Time travel ⏳", "Invisibility 🫥")),
        QuizQuestion("Go-to comfort food?", listOf("Noodles 🍜", "Pizza 🍕", "Momo 🥟", "Ice cream 🍨")),
        QuizQuestion("Morning person or night owl?", listOf("Early bird 🌅", "Night owl 🦉", "Depends ☯", "Always tired 😪")),
        QuizQuestion("Best gift to receive?", listOf("Something handmade 🎨", "Surprise trip 🧳", "Tech gadget 📱", "Just time together ⏰")),
        QuizQuestion("Karaoke song energy?", listOf("Romantic ballad 🎤", "Rock anthem 🎸", "Pop hit 💃", "Never singing 🙈")),
        QuizQuestion("Dream home?", listOf("City apartment 🏙", "Countryside house 🌾", "Beach cottage 🌊", "Mountain cabin 🌲")),
        QuizQuestion("Money windfall — first move?", listOf("Travel ✈", "Save it 🏦", "Shopping spree 🛍", "Gift for you ❤")),
        QuizQuestion("Party style?", listOf("Life of the party 🎉", "Small circle chat 🗣", "Quiet corner 🍹", "Rather stay home 🏡")),
        QuizQuestion("Scariest thing?", listOf("Spiders 🕷", "Heights 🪂", "Public speaking 🎙", "Nothing 😎")),
        QuizQuestion("Ideal date night?", listOf("Fancy dinner 🍷", "Street food walk 🌯", "Movie & cuddle 🛋", "Adventure day 🎢")),
        QuizQuestion("Phone habit?", listOf("Always replies fast ⚡", "Forgets phone exists 📵", "Endless scrolling 🌀", "Calls over texts 📞")),
        QuizQuestion("Cooking skills?", listOf("Chef level 👨‍🍳", "Can survive 🍚", "Instant noodles only 🍜", "Kitchen disaster 🔥")),
        QuizQuestion("Dream ride?", listOf("Motorbike 🏍", "Classic car 🚗", "Bicycle 🚲", "Chauffeur please 🧑‍✈️")),
        QuizQuestion("Weekend energy?", listOf("Out exploring 🥾", "Friends & family 👨‍👩‍👧", "Chores & order 🧹", "Full recharge mode 🔋")),
        QuizQuestion("Sweet or spicy?", listOf("Sweet tooth 🍬", "Spice lover 🌶", "Both, always 🤤", "Plain & simple 🍞")),
        QuizQuestion("Lost in a new city — what now?", listOf("Ask locals 🗣", "Map everything 🗺", "Wander happily 🚶", "Panic quietly 😅")),
        QuizQuestion("Favorite season?", listOf("Spring 🌸", "Summer ☀", "Autumn 🍁", "Winter ❄")),
        QuizQuestion("Game night pick?", listOf("Cards 🃏", "Video games 🎮", "Charades 🎭", "Just talking 💬")),
        QuizQuestion("Social media style?", listOf("Posts everything 📸", "Silent watcher 👀", "Meme sender 😹", "Barely online 🌿")),
    )

    fun indexForDate(dateKey: String): Int {
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        val epochDay = formatter.parse(dateKey)!!.time / 86_400_000L
        // stride + offset decorrelate this rotation from the daily question
        return ((epochDay * 7 + 3) % ALL.size).toInt()
    }

    fun forDate(dateKey: String): QuizQuestion = ALL[indexForDate(dateKey)]
}
