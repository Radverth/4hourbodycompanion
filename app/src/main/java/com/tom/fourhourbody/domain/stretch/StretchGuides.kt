package com.tom.fourhourbody.domain.stretch

/**
 * How to actually do each stretch.
 *
 * This is reference text, not user data, so it lives in code rather than in the database:
 * there is nothing here to edit, back up or migrate, and a correction ships with the next
 * build instead of needing a schema change.
 *
 * [setup] is where you get into position, [execution] is what you do once there, and
 * [watchFor] is the mistake that makes the stretch do nothing — which is the part usually
 * left out, and the reason a stretch held for ninety seconds can still achieve nothing.
 */
data class StretchGuide(
    val setup: List<String>,
    val execution: List<String>,
    val watchFor: String
)

object StretchGuides {

    private val byName: Map<String, StretchGuide> = mapOf(
        "Hip flexor stretch" to StretchGuide(
            setup = listOf(
                "Half-kneel: one knee down on a mat, the other foot flat in front, both knees " +
                    "at about 90°.",
                "Stack your shoulders over your hips so your torso is upright, not leaning."
            ),
            execution = listOf(
                "Squeeze the glute on the kneeling side first. That tucks the pelvis under and " +
                    "is what actually lengthens the hip flexor.",
                "Keeping that squeeze, shift your weight forward a couple of centimetres only.",
                "Raise the arm on the kneeling side overhead and reach slightly to the opposite " +
                    "side."
            ),
            watchFor = "Arching the lower back to feel more stretch. That moves the stretch into " +
                "your spine and away from the hip — if your ribs flare, reset and squeeze the " +
                "glute again."
        ),

        "Double-leg glute bridge" to StretchGuide(
            setup = listOf(
                "Lie on your back, knees bent, feet flat and about hip-width apart.",
                "Heels close enough that your fingertips almost brush them."
            ),
            execution = listOf(
                "Push through your heels and lift your hips until shoulders, hips and knees " +
                    "make one straight line.",
                "Squeeze the glutes hard at the top and hold for a beat.",
                "Lower under control — this is activation work, so the lowering counts."
            ),
            watchFor = "Feeling it in your hamstrings or lower back instead of your glutes. " +
                "Usually the feet are too far away; walk them closer to your hips."
        ),

        "Single-leg glute bridge" to StretchGuide(
            setup = listOf(
                "Same position as the double-leg bridge.",
                "Lift one knee towards your chest and hold it there, or extend that leg straight."
            ),
            execution = listOf(
                "Drive through the heel of the planted foot and lift your hips.",
                "Keep your hips level — both hip bones should stay at the same height.",
                "Complete all reps on one side, then swap."
            ),
            watchFor = "The hip on the free-leg side dropping as you tire. When it starts to " +
                "drop, that set is finished, whatever the rep count says."
        ),

        "Super quad (couch) stretch" to StretchGuide(
            setup = listOf(
                "Kneel with your back to a couch, wall or bench.",
                "Slide one shin up the vertical surface so the knee sits in the corner and the " +
                    "foot points up.",
                "Bring the other foot flat in front of you in a half-kneel."
            ),
            execution = listOf(
                "Squeeze the glute of the trapped leg to bring the pelvis underneath you.",
                "Only then raise your torso towards upright, a little at a time.",
                "Breathe out slowly and let each exhale take you a few millimetres further."
            ),
            watchFor = "Going upright before you have tucked the pelvis. It looks like a deeper " +
                "stretch and is mostly lower-back compression."
        ),

        "Pelvic symmetry / glute flexibility" to StretchGuide(
            setup = listOf(
                "Sit on the floor with one leg crossed in front, shin roughly parallel to your " +
                    "chest.",
                "Extend the other leg straight behind you, that thigh flat to the floor."
            ),
            execution = listOf(
                "Keep both hips square to the front — that squareness is the whole exercise.",
                "Walk your hands forward and lower your chest towards the front shin.",
                "Hold, breathing out into the position rather than pushing into it.",
                "The named positions change the front shin angle; work each one before swapping."
            ),
            watchFor = "Letting the back hip rotate open. If it lifts away from the floor you " +
                "are stretching a different muscle and losing the symmetry the routine is for."
        ),

        "Pelvis repositioning" to StretchGuide(
            setup = listOf(
                "Lie on your back with both knees bent and feet flat.",
                "Follow the side and position the routine names for this entry."
            ),
            execution = listOf(
                "Press gently and evenly rather than forcing — this resets position, it does " +
                    "not stretch tissue.",
                "Keep both shoulders flat on the floor throughout.",
                "Hold for the full time; the change happens slowly and late."
            ),
            watchFor = "Holding your breath. Bracing locks the pelvis exactly where you are " +
                "trying to let it move."
        ),

        "Static Back" to StretchGuide(
            setup = listOf(
                "Lie on your back on the floor and rest your lower legs on a chair, sofa or box.",
                "Hips and knees both at about 90°.",
                "Arms out to the sides at roughly 45°, palms up."
            ),
            execution = listOf(
                "Do nothing. Let your lower back settle towards the floor under its own weight.",
                "Breathe normally and stay for the full time — five minutes is the dose.",
                "Expect the settling to happen in the last third, not the first."
            ),
            watchFor = "Cutting it short because nothing seems to be happening. Nothing " +
                "happening is the point; this one works by time, not effort."
        ),

        "Static Extension on Elbows" to StretchGuide(
            setup = listOf(
                "On hands and knees, then walk your elbows forward until they sit under your " +
                    "shoulders or a little ahead.",
                "Hips stay stacked over your knees."
            ),
            execution = listOf(
                "Let your lower back sag towards the floor and your shoulder blades draw " +
                    "together.",
                "Let your head hang rather than holding it up.",
                "Hold and keep breathing; the sag should deepen slowly."
            ),
            watchFor = "Hips drifting back towards your heels, which turns this into a rest " +
                "position and removes the extension entirely."
        ),

        "Shoulder Bridge with Pillow" to StretchGuide(
            setup = listOf(
                "Lie on your back, knees bent, feet flat and hip-width apart.",
                "Put a pillow or block between your knees."
            ),
            execution = listOf(
                "Squeeze the pillow just enough to keep it in place — light, constant pressure.",
                "Lift your hips into a bridge, keeping that squeeze.",
                "Hold at the top, then lower with the pillow still under tension."
            ),
            watchFor = "Letting the knees splay as you lift. If the pillow goes slack the " +
                "adductors have stopped working and the exercise has changed."
        ),

        "Active Bridges with Pillow" to StretchGuide(
            setup = listOf(
                "Same setup as the shoulder bridge, pillow between the knees.",
                "This is the weekly, longer version rather than the interval set."
            ),
            execution = listOf(
                "Squeeze the pillow, lift the hips, hold for a second at the top.",
                "Lower fully so your hips touch down between reps.",
                "Keep a steady rhythm — these are meant to be repeated, not held."
            ),
            watchFor = "Rushing so the hips never fully return. The reset at the bottom is what " +
                "makes each rep a repetition rather than a pulse."
        ),

        "Supine Groin Progressive" to StretchGuide(
            setup = listOf(
                "Lie on your back with one leg resting on a block or chair, hip and knee at 90°.",
                "Extend the other leg flat along the floor, foot relaxed and falling naturally."
            ),
            execution = listOf(
                "Stay still and let the extended leg's hip release towards the floor.",
                "Hold for the full time, then lower the supported leg a level and hold again.",
                "Work down through the levels before swapping sides."
            ),
            watchFor = "Turning the extended foot inward. Let it fall outward naturally — " +
                "correcting it holds the hip in the position you are trying to release."
        ),

        "Air Bench" to StretchGuide(
            setup = listOf(
                "Stand with your back flat against a wall, feet hip-width and well forward.",
                "Slide down until your thighs are as close to parallel with the floor as you " +
                    "can hold."
            ),
            execution = listOf(
                "Keep your lower back flat to the wall and your weight in your heels.",
                "Ankles, knees and hips all at roughly 90°.",
                "Hold. This is meant to burn; keep breathing through it."
            ),
            watchFor = "Weight sliding onto your toes as you tire, which takes the load off the " +
                "quads and puts it into your knees."
        )
    )

    operator fun get(stretchName: String): StretchGuide? = byName[stretchName]

    fun has(stretchName: String): Boolean = byName.containsKey(stretchName)

    /** Every stretch the app seeds should be documented; the test asserts that. */
    val documented: Set<String> get() = byName.keys
}
