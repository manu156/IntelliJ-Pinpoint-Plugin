package com.github.manu156.pinpointintegration.editor.util;

import com.github.manu156.pinpointintegration.editor.RenderQueue;
import com.github.manu156.pinpointintegration.editor.data.EffectType;
import com.github.manu156.pinpointintegration.common.dto.Dto;
import com.github.manu156.pinpointintegration.editor.render.RenderData;
import com.github.manu156.pinpointintegration.editor.render.MyElementRenderer;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.impl.DocumentMarkupModel;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.manu156.pinpointintegration.common.util.CStringUtil.strip;
import static com.github.manu156.pinpointintegration.common.util.CStringUtil.stripLeading;

public class RenderUtil {



    public static void renderHelper(List<JsonObject> js, FileEditor[] fileEditors, @NotNull Project project) {
        // @NotNull Color textColor, @NotNull Color backgroundColor, @NotNull Color effectColor
        Color[] colorConfig = new Color[]{
                new Color(44, 181, 51),
                Color.BLACK,
                Color.BLACK
        };
//        Color[] colorConfig = new Color[]{
//                new Color(173, 38, 119),
//                Color.BLACK,
//                Color.BLACK
//        };


        RenderQueue rq = ApplicationManager.getApplication().getService(RenderQueue.class);
        Disposable disposable = Disposer.newDisposable();
        rq.addToDisposable(disposable);
        for (int jsIndex=0; jsIndex<js.size(); jsIndex++) {
            boolean apiControllerRendered = false;
            Dto dto = parseData(js.get(jsIndex));
            Map<String, Map<String, Integer>> classNamesToMethodSigToIndex = dto.classNamesToMethodSigToIndex;
            JsonArray callStackJson = dto.callStackJson;
            JsonObject callStackIndexJson = dto.callStackIndexJson;
            Map<Integer, List<Integer>> parentToChildIndex = dto.parentToChildIndex;
            for (FileEditor fileEditor : fileEditors) {
                if (!(fileEditor instanceof TextEditor)) continue;
                TextEditor textEditor = (TextEditor) fileEditor;
                Editor editor = textEditor.getEditor();

                MarkupModel markupModel = DocumentMarkupModel.forDocument(
                        textEditor.getEditor().getDocument(), textEditor.getEditor().getProject(), false);

                PsiFile pf = PsiManager.getInstance(textEditor.getEditor().getProject()).findFile(fileEditor.getFile());
                PsiClass[] cls = PsiTreeUtil.getChildrenOfType(pf, PsiClass.class);
                if (null == cls)
                    continue;
                for (PsiClass cl : cls) {
                    PsiAnnotation controllerAnn = cl.getAnnotation("org.springframework.web.bind.annotation.RequestMapping");
                    Set<String> pathList = new HashSet<>();
                    if (null != controllerAnn && null != dto.apiEndPoint) {
                        //todo: value is array, there could be multiple values
                        List<PsiNameValuePair> pathPsis = Arrays.stream(controllerAnn.getParameterList().getAttributes())
                                .toList();
                        for (PsiNameValuePair pathPsi : pathPsis) {
                            List<String> ce = getValuesByKey(pathPsi, "value", "path").stream()
                                    .map(t -> "/" + stripLeading(t, "/")).toList();
                            // todo: do proper filtering

                            pathList.addAll(ce.stream()
                                    .filter(t -> ("/"+stripLeading(dto.apiEndPoint, "/")).startsWith(t))
                                    .toList());
                        }
                    }

                    if (pathList.isEmpty() && null != dto.apiEndPoint) {
                        PsiAnnotation restControllerAnn = cl.getAnnotation("org.springframework.web.bind.annotation.RestController");
                        if (null != restControllerAnn) {
                            pathList.add("");
                        }
                    }

                    Map<String, Integer> dataMap = classNamesToMethodSigToIndex.getOrDefault(cl.getName(), Collections.emptyMap());
                    if (dataMap.isEmpty() && pathList.isEmpty())
                        continue;

                    for (Map.Entry<String, Integer> entry : dataMap.entrySet()) {
                        String[] mspl = StringUtils.split(entry.getKey(), "(", 2);
                        String methodName = mspl[0];
                        List<String> params = Arrays.stream(StringUtils.split(mspl[1].substring(0, mspl[1].length() - 1), ","))
                                .map(String::strip).toList();
                        PsiMethod[] ms = cl.findMethodsByName(methodName, false);
                        if (ms.length < 1)
                            continue;
                        PsiMethod finalMethod = null;
                        int finalMatch = 0;
                        for (PsiMethod m : ms) {
                            if (m.getParameters().length != params.size())
                                continue;
                            PsiParameterList psiParamList = m.getParameterList();
                            int currentMatch = 0;
                            for (int i = 0; i < psiParamList.getParametersCount(); i++) {
                                String[] dataParam = StringUtils.split(params.get(i), " ");
                                PsiParameter parameter = psiParamList.getParameter(i);
                                if (dataParam[0].equals(parameter.getTypeElement().getType().getPresentableText()))
                                    currentMatch++;
                                if (dataParam[1].equals(parameter.getName()))
                                    currentMatch++;
                            }
                            if (currentMatch >= finalMatch) {
                                finalMethod = m;
                                finalMatch = currentMatch;
                            }
                        }
                        if (null == finalMethod)
                            continue;

                        apiControllerRendered = renderForControllerMethod(finalMethod, pathList, dto,
                                apiControllerRendered, colorConfig, editor, disposable);


                        int reqOffset = finalMethod.getTextOffset();
                        JsonArray currJson = callStackJson.get(entry.getValue()).getAsJsonArray();
                        String execTime = currJson.get(callStackIndexJson.get("elapsedTime").getAsInt()).getAsString();
                        MyElementRenderer rt = new MyElementRenderer(new RenderData(
                                false, true, false,
                                true, colorConfig[0], colorConfig[1], colorConfig[2],
                                1, EffectType.NONE,
                                execTime + "ms", AllIcons.General.Gear));
                        @Nullable Inlay<MyElementRenderer> tin = editor.getInlayModel().addAfterLineEndElement(
                                reqOffset, false, rt);
                        if (null != tin)
                            Disposer.register(disposable, tin);


                        if (CollectionUtils.isNotEmpty(parentToChildIndex.get(entry.getValue()))) {
                            List<PsiMethodCallExpression> psiCalls = PsiTreeUtil.findChildrenOfType(finalMethod.getBody(), PsiMethodCallExpression.class).stream().toList();
                            if (null != psiCalls) {
                                int ic = 0;
                                for (Integer childIndex : parentToChildIndex.get(entry.getValue())) {
                                    JsonArray cj = callStackJson.get(childIndex).getAsJsonArray();
                                    String mName = StringUtils.split(cj.get(callStackIndexJson.get("title").getAsInt()).getAsString(), "(", 2)[0];

                                    int pc = ic;
                                    while (pc < psiCalls.size()) {
                                        String pName = psiCalls.get(pc).getMethodExpression().getQualifiedName();
                                        if (pName.contains(".")) {
                                            String[] spl = StringUtils.split(pName, ".");
                                            pName = spl[spl.length - 1];
                                        }
                                        if (!mName.equalsIgnoreCase(pName)) {
                                            pc++;
                                            continue;
                                        }

                                        String execSub = cj.get(callStackIndexJson.get("elapsedTime").getAsInt()).getAsString();
                                        MyElementRenderer rt2 = new MyElementRenderer(new RenderData(
                                                false, true, false,
                                                true,
                                                colorConfig[0], colorConfig[1], colorConfig[2],
                                                1, EffectType.NONE,
                                                execSub + "ms", AllIcons.General.Gear));
                                        @Nullable Inlay<MyElementRenderer> tin2 = editor.getInlayModel().addAfterLineEndElement(
                                                psiCalls.get(pc).getTextOffset(), false, rt2);
                                        if (null != tin2)
                                            Disposer.register(disposable, tin2);
                                        ic++;
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    if (!apiControllerRendered && !pathList.isEmpty()) {
                        PsiMethod[] ms = cl.getAllMethods();
                        for (PsiMethod m : ms) {
                            renderForControllerMethod(m, pathList, dto, apiControllerRendered, colorConfig, editor, disposable);
                        }
                    }
                }

            }
        }
    }

    private static boolean renderForControllerMethod(PsiMethod finalMethod, Set<String> pathList, Dto dto, boolean apiControllerRendered, Color[] colorConfig, Editor editor, Disposable disposable) {
        PsiAnnotation[] methodAnnotations = finalMethod.getAnnotations();
        for (String controllerPath : pathList) {
            for (PsiAnnotation methodAnnotation : methodAnnotations) {
                Set<String> apiMethodsQns = new HashSet<>(Arrays.asList(
                        "org.springframework.web.bind.annotation.RequestMapping",
                        "org.springframework.web.bind.annotation.GetMapping",
                         "org.springframework.web.bind.annotation.PostMapping",
                         "org.springframework.web.bind.annotation.PutMapping",
                         "org.springframework.web.bind.annotation.DeleteMapping",
                         "org.springframework.web.bind.annotation.PatchMapping"
                ));
                if (!apiMethodsQns.contains(methodAnnotation.getQualifiedName()))
                    continue;
                List<PsiNameValuePair> pv = Arrays.stream(methodAnnotation.getParameterList().getAttributes())
                        .toList();
                PsiNameValuePair finalPv = null;
                for (PsiNameValuePair valuePair : pv) {
                    List<String> ce = getValuesByKey(valuePair, "value", "path");

                    // todo: do proper filtering
                    if (ce.stream().anyMatch(t -> dto.apiEndPoint.equals(controllerPath+t))) {
                        finalPv = valuePair;
                    }
                }
                if (null != finalPv) {
                    apiControllerRendered = true;
                    int reqOffset = finalPv.getTextOffset();
                    String execTime = "" + dto.apiTotalTime;
                    MyElementRenderer rt = new MyElementRenderer(new RenderData(
                            false, true, false,
                            true, colorConfig[0], colorConfig[1], colorConfig[2],
                            1, EffectType.NONE,
                            execTime + "ms", AllIcons.General.Gear));
                    @Nullable Inlay<MyElementRenderer> tin = editor.getInlayModel().addAfterLineEndElement(
                            reqOffset, false, rt);
                    if (null != tin)
                        Disposer.register(disposable, tin);
                }
            }
        }
        return apiControllerRendered;
    }

    public static List<String> getValuesByKey(@NotNull PsiNameValuePair pair, @NotNull String ...name) {
        Set<String> keys = new HashSet<>(List.of(name));

        if (null != pair.getLiteralValue() && (keys.contains(pair.getName()) || keys.contains(pair.getAttributeName())))
            return Collections.singletonList(pair.getLiteralValue());
        else if (null != pair.getValue() && (keys.contains(pair.getName()) || keys.contains(pair.getAttributeName()))) {
            return getStringList(pair);
        }

        return new ArrayList<>();
    }

    private static List<String> getStringList(PsiNameValuePair pair) {

        if (pair.getValue() instanceof PsiArrayInitializerMemberValue)
            return Arrays.stream(((PsiArrayInitializerMemberValue) pair.getValue()).getInitializers())
                    .map(t -> strip(strip(t.getText(), '"'), "\\n"))
                    .toList();
        else {
            String text = Objects.requireNonNull(pair.getValue()).getText();
            return Collections.singletonList(strip(text, '"'));
        }
    }

    public static Dto parseData(JsonObject jsonObject) {
        Map<String, Map<String, Integer>> classNamesToMethodSigToIndex = new HashMap<>();
//        JsonObject callStackIndex;
//        JsonArray callStack;
//        Map<Long, JsonArray> idToTransaction = new HashMap<>();
        Map<Integer, List<Integer>> parentToChildIndex = new HashMap<>();
        Map<Integer, Integer> idToIndex = new HashMap<>();

        if (!jsonObject.has("callStackIndex") || !jsonObject.has("callStack"))
            return null;
        JsonObject callStackIndexJson = jsonObject.get("callStackIndex").getAsJsonObject();
        JsonArray callStackJson = jsonObject.get("callStack").getAsJsonArray();
        if (callStackJson.size() < 1)
            return null;
        JsonArray first = callStackJson.get(0).getAsJsonArray();
        String apiName = first.get(callStackIndexJson.get("arguments").getAsInt()).getAsString();
        Long apiTotalTime = first.get(callStackIndexJson.get("elapsedTime").getAsInt()).getAsLong();
        int counter = 0;
        for (JsonElement jsonElement: callStackJson) {
            JsonArray row = jsonElement.getAsJsonArray();
            idToIndex.put(row.get(callStackIndexJson.get("id").getAsInt()).getAsInt(), counter);
            counter++;
        }
        counter = 0;
        for (JsonElement jsonElement: callStackJson) {
            JsonArray row = jsonElement.getAsJsonArray();
            String pid = row.get(callStackIndexJson.get("parentId").getAsInt()).getAsString();
            if (!StringUtils.isEmpty(pid)) {
                int pIndex = idToIndex.get(Integer.parseInt(pid));
                if (!parentToChildIndex.containsKey(pIndex))
                    parentToChildIndex.put(pIndex, new ArrayList<>());
                parentToChildIndex.get(pIndex).add(counter);
            }
            if (!"true".equalsIgnoreCase(row.get(callStackIndexJson.get("isMethod").getAsInt()).getAsString())) {
                counter++;
                continue;
            }

            String className = row.get(callStackIndexJson.get("simpleClassName").getAsInt()).getAsString();
//            String[] methodCall = StringUtils.split(row.get(callStackIndexJson.get("title").getAsInt()).getAsString(),
//                    "(", 1);
//            String methodName = methodCall[0];
            String methodSig = row.get(callStackIndexJson.get("title").getAsInt()).getAsString();
            if (StringUtils.isBlank(className) || StringUtils.isBlank(methodSig)) {
                counter++;
                continue;
            }
            if (!classNamesToMethodSigToIndex.containsKey(className))
                classNamesToMethodSigToIndex.put(className, new HashMap<>());
//            if (!classNamesToMethodSigToIndex.get(className).containsKey(methodName))
//                classNamesToMethodSigToIndex.get(className).put(methodName, new HashSet<>());
            classNamesToMethodSigToIndex.get(className).put(methodSig, counter);

            counter++;
        }
        return new Dto(callStackJson, classNamesToMethodSigToIndex, callStackIndexJson, parentToChildIndex, apiName, apiTotalTime);
    }
}
