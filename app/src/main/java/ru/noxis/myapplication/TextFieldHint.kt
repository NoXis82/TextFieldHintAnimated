package ru.noxis.myapplication

import android.provider.CalendarContract
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun TextFieldHint(
    modifier: Modifier = Modifier,
    hint: String = "hint",
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    var text by remember { mutableStateOf("") }

    /**
     * значения, чтобы отслеживать состояние нашего текстового поля
     * когда текстовое поле находится в фокусе или содержит текст,
     * чтобы определить, нужно ли размещать подсказку внутри или над полем
     */
    val interactionSource = remember { MutableInteractionSource() }

    val isFocused by interactionSource.collectIsFocusedAsState()
    val showHintAbove by remember {
        derivedStateOf {
            isFocused || text.isNotEmpty()
        }
    }

    BasicTextField(
        value = text,
        onValueChange = { text = it },
        interactionSource = interactionSource,

        visualTransformation = visualTransformation,
        textStyle = textFieldTextStyle(),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),

        decorationBox = { innerTextField ->
            //давайте добавим к нему анимацию
            /**
             * Для любых общих переходов элементов нам нужно сначала создать
             * SharedTransitionLayout и AnimatedContent внутри него.
             */
            SharedTransitionLayout {
                AnimatedContent(
                    targetState = showHintAbove, //используем showHintAbove в качестве targetState
                    transitionSpec = {
                        //Для перехода нам не нужна анимация, поэтому мы переходим без перехода
                        EnterTransition.None togetherWith ExitTransition.None
                    },
                    label = "hintAnimation"
                ) { showHintAbove ->
                    //добавляем Column с заголовком нашего текстового поля
                    Column {
                        Box(Modifier.padding(start = 2.dp)) {
                            if (showHintAbove) {
                                //используем супер-пользовательский TextAsIndividualLetters элемент
                                TextAsIndividualLetters(
                                    animatedContentScope = this@AnimatedContent,
                                    text = hint,
                                    style = exteriorHintTextStyle(),
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        //отобразить само текстовое поле
                        //Box элемент, который можно стилизовать по своему усмотрению

                        Box(
                            modifier = Modifier
                                //нам нужно установить модификатор sharedElement для всего элемента
                                //чтобы не создавать несколько текстовых полей во время анимации
                                .sharedElement(
                                    rememberSharedContentState(key = "input"),
                                    animatedVisibilityScope = this@AnimatedContent
                                )
                                .defaultMinSize(minWidth = 300.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .border(
                                    width = Dp.Hairline,
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = .3f
                                    )
                                )
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (!showHintAbove) {
                                //добавляем текстовую подсказку, которая будет отображаться в текстовом поле
                                TextAsIndividualLetters(
                                    animatedContentScope = this@AnimatedContent,
                                    text = hint,
                                    style = interiorHintTextStyle(),
                                )
                            }
                            //можем отображать innerTextField так, чтобы текст отображался по мере ввода пользователем
                            innerTextField()
                        }
                    }
                }

            }

        }

    )
}


@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.TextAsIndividualLetters(
    animatedContentScope: AnimatedContentScope,
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle(),
) {
    /**
     * разбивает наш текст на отдельные буквы
     */
    Row(modifier) {
        //разместить каждую букву в отдельном Text компоненте внутри Row
        text.forEachIndexed { index, letter ->
            Text(
                text = "$letter",
                modifier = Modifier.sharedBounds(
                    //К каждому Text применяется модификатор sharedBounds с индексом буквы, используемым в ключе
                    sharedContentState = rememberSharedContentState(key = "hint_$index"),
                    animatedVisibilityScope = animatedContentScope,
                    //Чтобы добиться неравномерной анимации букв, нам нужно определить boundsTransform
                    boundsTransform = { _, _ ->
                        /**
                         * Мы возвращаем spring в котором настраиваем stiffness так,
                         * чтобы первые буквы имели более высокое значение, чем последние
                         */
                        spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = 25f * (text.length - index),
                        )
                    }
                ),
                style = style,
            )
        }
    }
}

@ReadOnlyComposable
@Composable
private fun exteriorHintTextStyle() = MaterialTheme.typography.labelLarge.copy(
    fontWeight = FontWeight.Bold,
    fontSize = 12.sp,
    color = Color.Green,
)

@ReadOnlyComposable
@Composable
private fun interiorHintTextStyle() = textFieldTextStyle().copy(
    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .4f),
)

@ReadOnlyComposable
@Composable
private fun textFieldTextStyle() = MaterialTheme.typography.labelLarge.copy(
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .9f),
)