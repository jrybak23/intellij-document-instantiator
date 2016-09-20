package com.company.document.instantiator.actions;

import com.company.document.instantiator.model.CompilationFile;
import com.company.document.instantiator.util.*;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;

import javax.xml.bind.JAXBException;

/**
 * Created by igorek2312 on 20.09.16.
 */
public abstract class InstantiateDocumentAction extends AnAction {

    @Override
    public void update(AnActionEvent e) {
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        boolean visible = file != null && file.getExtension().equals("java");

        e.getPresentation().setEnabledAndVisible(visible);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        ApplicationManager.getApplication().saveAll();
        Project project = e.getData(PlatformDataKeys.PROJECT);
        ProjectFileIndex prIndex = ProjectRootManager.getInstance(project).getFileIndex();

        VirtualFile currentFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        VirtualFile sourceRoot = prIndex.getSourceRootForFile(currentFile);
        String packageName = prIndex.getPackageNameByDirectory(currentFile.getParent());
        String className = (packageName.isEmpty() ? "" : (packageName + ".")) + currentFile.getNameWithoutExtension();

        CompilationFile compilationFile = new CompilationFile(
                sourceRoot.getPath(),
                currentFile.getPath(),
                className,
                currentFile.getParent().getPath()
        );

        String successMessage = null;
        DocumentConventer documentConventer = null;
        if (this instanceof InstantiateJsonAction) {
            successMessage = "JSON copied to clipboard";
            documentConventer = new JacksonDocumentConventer();
        } else if (this instanceof InstantiateXmlAction) {
            successMessage = "XML copied to clipboard";
            documentConventer = new JaxbDocumentConventer();
        }

        try (ClasspathLoader classpathLoader = new JavaClasspathLoader(compilationFile)) {
            Object instance = classpathLoader.compileAndInstantiate();
            ObjectInitializer objectInitializer = new ObjectInitializerimpl();
            objectInitializer.initializeObject(instance);
            String document = documentConventer.convertFromPojo(instance);
            ClipboardUtil.getInstance().copyToClipBoard(document);
            Messages.showMessageDialog(project, successMessage, "Inforamtion", Messages.getInformationIcon());
        } catch (InstantiationException e1) {
            Messages.showMessageDialog(project, "The class cannot be instantiated", "Error", Messages.getErrorIcon());
        } catch (JAXBException e1) {
            Messages.showMessageDialog(project, e1.getLinkedException().getMessage(), "Error", Messages.getErrorIcon());
        } catch (Exception e1) {
            Messages.showMessageDialog(project, e1.getMessage(), "Error", Messages.getErrorIcon());
            e1.printStackTrace();
        }
    }
}