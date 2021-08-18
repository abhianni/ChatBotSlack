package mmt.slack.helper;

import com.slack.api.bolt.context.builtin.ActionContext;
import com.slack.api.bolt.context.builtin.EventContext;
import com.slack.api.model.block.ImageBlock;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;
import com.slack.api.model.block.composition.PlainTextObject;
import com.slack.api.model.block.composition.TextObject;
import com.slack.api.model.block.element.BlockElement;
import com.slack.api.model.block.element.BlockElements;
import com.slack.api.model.block.element.ImageElement;
import com.slack.api.model.view.View;
import mmt.slack.pojo.AutomationTriggerDTO;
import mmt.slack.pojo.DockerResponse;
import mmt.slack.pojo.DockerServiceListResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.*;
import static com.slack.api.model.block.composition.BlockCompositions.option;
import static com.slack.api.model.block.composition.BlockCompositions.plainText;
import static com.slack.api.model.block.element.BlockElements.*;
import static com.slack.api.model.view.Views.*;
@Service
public class Views {

   public View buildViewForAutomation(String externalID) {
        return view(view -> view
                .callbackId(externalID+"_Automation1")
                .type("modal")
                .externalId(externalID+"_Automation2")
                .notifyOnClose(true)
                .clearOnClose(true)
                .title(viewTitle(title -> title.type("plain_text").text("AutomationCommand").emoji(true)))
                .submit(viewSubmit(submit -> submit.type("plain_text").text("Submit").emoji(true)))
                .close(viewClose(close -> close.type("plain_text").text("Cancel").emoji(true)))
                .blocks(asBlocks(
                        section(section -> section
                                .blockId("project-block")
                                .text(markdownText("*Pick a Project from the dropdown list*"))
                                .accessory(staticSelect(staticSelect -> staticSelect
                                        .actionId("project-selection-action")
                                        .placeholder(plainText("Select Project to be trigger"))
                                        .options(asOptions(
                                                option(plainText("HES"), "HES"),
                                                option(plainText("CG"), "CG")
                                        ))
                                ))
                        ),

                        input(input -> input
                                .blockId("branch-block")
                                .element(plainTextInput(pti -> pti.actionId("branch-action").multiline(false)))
                                .label(plainText(pt -> pt.text("Branch Name").emoji(true)))
                                .optional(true)
                                .dispatchAction(true)

                        ),
                        actions(actions -> actions
                                .elements(asElements(
                                        button(b -> b.text(plainText(pt -> pt.emoji(true).text("Submit"))).value("submit").actionId("submit"))

                                )
                                )

                        )
                ))
        );
    }

  public  View buildViewForEdge(String externalID) {
        return view(view -> view
                .callbackId(externalID+"_Edge")
                .type("modal")
                .notifyOnClose(true)
                .externalId(externalID+"_Edge")
                .clearOnClose(true)
                .title(viewTitle(title -> title.type("plain_text").text("EdgeCommand").emoji(true)))
                .submit(viewSubmit(submit -> submit.type("plain_text").text("Submit").emoji(true)))
                .close(viewClose(close -> close.type("plain_text").text("Cancel").emoji(true)))
                .blocks(asBlocks(
                        section(section -> section
                                .blockId("project-block")
                                .text(markdownText("*Pick a Project from the dropdown list*"))
                                .accessory(staticSelect(staticSelect -> staticSelect
                                        .actionId("project-selection-action")
                                        .placeholder(plainText("Select Project to be trigger"))
                                        .options(asOptions(
                                                option(plainText("HES"), "HES"),
                                                option(plainText("CG"), "CG")
                                        ))
                                ))
                        ),

                        input(input -> input
                                .blockId("user-block")
                                .element(plainTextInput(pti -> pti.actionId("userId-action").multiline(false)))
                                .label(plainText(pt -> pt.text("UserId").emoji(true)))
                                .optional(true)
                                .dispatchAction(true)
                        )

                        ,
                        input(input -> input
                                .blockId("psswrd-block")
                                .element(plainTextInput(pti -> pti.actionId("psswrd-action").multiline(false)))
                                .label(plainText(pt -> pt.text("Passwrd(Your passwrd is secure and will be used to trigger Edge job)").emoji(true)))
                                .optional(true)
                                .dispatchAction(true)
                        ),
                        actions(actions -> actions
                                .elements(asElements(
                                        button(b -> b.text(plainText(pt -> pt.emoji(true).text("Submit"))).value("submit").actionId("submit"))

                                        )
                                ))
                ))
        );
    }


    public View buildResponseView(List<LayoutBlock > resultBlock, String externalID, View existingView) {
        return view(view -> view
                .callbackId(externalID+"_Response")
                .type("modal")
                .externalId(externalID+"_Response")
                .notifyOnClose(true)
                .clearOnClose(true)
                .title(viewTitle(title -> title.type("plain_text").text("CommandResult").emoji(true)))
                .submit(viewSubmit(submit -> submit.type("plain_text").text("submit").emoji(true)))
                .close(viewClose(close -> close.type("plain_text").text(existingView.getCallbackId()).emoji(true)))
                .blocks(resultBlock)
        );
    }

    public LayoutBlock imageBlock(String url)
    {
        return ImageBlock
                .builder()
                .blockId("image-block")
                .altText("Execution History")
                .imageUrl(url)
                .title((PlainTextObject.builder().text("Test Execution History").build()))
                .build();
    }

    public LayoutBlock buildResponseDataEdge(AutomationTriggerDTO userRequest)
    {
        return  SectionBlock
                .builder()
                .text(MarkdownTextObject.builder().text("Requested Command").build())
                .fields(Arrays.asList(
                        MarkdownTextObject
                                .builder()
                                .text("*Execution Requested:* " + userRequest.getCommand())
                                .build(),
                        MarkdownTextObject
                                .builder()
                                .text("*Project Selected:* " + userRequest.getComponent())
                                .build()
                        )
                )
                .build();
    }

    public LayoutBlock buildResponseDocker(AutomationTriggerDTO userRequest, List<DockerResponse> dockerResponseList)
    {
        List<TextObject> textObjectList = new ArrayList<>();
        for(DockerResponse docker :dockerResponseList)
        {
            textObjectList.add(MarkdownTextObject.builder()
                    .text("\n*ImageName:* "+docker.getImage().split("/")[1]).build());
            textObjectList.add(MarkdownTextObject.builder()
                    .text("*State:* "+docker.getState() +" "+docker.getStatus()).build());
            textObjectList.add(MarkdownTextObject.builder()
                    .text("*ContainerName:* "+docker.getNames().get(0).substring(1,docker.getNames().get(0).length())
                   )
                    .build());

        }
        return  SectionBlock
                .builder()
                .text(MarkdownTextObject.builder().text("*Server Container Details*").build())
                .fields(textObjectList
                )
                .build();
    }

    public LayoutBlock buildResponseDataAutomation(AutomationTriggerDTO userRequest)
    {
        return  SectionBlock
                .builder()
                .text(MarkdownTextObject.builder().text("Requested Command").build())
                .fields(Arrays.asList(
                        MarkdownTextObject
                                .builder()
                                .text("*Execution Requested:* " + userRequest.getCommand())
                                .build(),
                        MarkdownTextObject
                                .builder()
                                .text("*Project Selected:* " + userRequest.getComponent())
                                .build(),
                        MarkdownTextObject
                                .builder()
                                .text("*Branch Provided* " +userRequest.getBranch() )
                                .build(),
                        MarkdownTextObject
                                .builder()
                                .text("*Total Tests* " +userRequest.getTotalCount() )
                                .build(),
                        MarkdownTextObject
                                .builder()
                                .text("*Failed Tests* " +userRequest.getFailCount() )
                                .build(),
                        MarkdownTextObject
                                .builder()
                                .text("*Skip Tests* " +userRequest.getSkipCount() )
                                .build()
                        )
                )
                .build();
    }

    public List<LayoutBlock>  buildRequestForAutomation(String userId)
    {
        List<LayoutBlock> message = new ArrayList();
        message.add(SectionBlock
                .builder()
                .text(MarkdownTextObject
                        .builder()
                        .text("Hi <@" + userId + "> " + "Please fill below details")
                        .build())
                .build());
        message.add( section(section -> section
                .blockId("Request-automation-block")
                .text(markdownText("*Pick a Project from the dropdown list*"))
                .accessory(staticSelect(staticSelect -> staticSelect
                        .actionId("request-selection-action")
                        .placeholder(plainText("Select Project to be trigger"))
                        .options(asOptions(
                                option(plainText("HES"), "HES"),
                                option(plainText("CG"), "CG")
                        ))
                ))
        ));
      message.add( input(input -> input
                      .blockId("branch-request-block")
                      .element(plainTextInput(pti -> pti.actionId("branch-request-action").multiline(false)))
                      .label(plainText(pt -> pt.text("Project Branch").emoji(true)))
                      .optional(true)
                      .dispatchAction(true)
              ));
        message.add( actions(actions -> actions
                .elements(asElements(
                        button(b -> b.text(plainText(pt -> pt.emoji(true).text("Submit"))).value("submit").actionId("submitRequest")))
                )));
        return message;
    }

    public List<LayoutBlock>  buildRequestForEdge(String userId)
    {
        List<LayoutBlock> message = new ArrayList();
        message.add(SectionBlock
                .builder()
                .text(MarkdownTextObject
                        .builder()
                        .text("Hi <@" + userId + "> " + "Please fill below details")
                        .build())
                .build());
        message.add( section(section -> section
                .blockId("Request-automation-block")
                .text(markdownText("*Pick a Project from the dropdown list*"))
                .accessory(staticSelect(staticSelect -> staticSelect
                        .actionId("request-selection-action")
                        .placeholder(plainText("Select Project to be trigger"))
                        .options(asOptions(
                                option(plainText("HES"), "HES"),
                                option(plainText("CG"), "CG")
                        ))
                ))
        ));
        message.add( input(input -> input
                .blockId("usrname-request-block")
                .element(plainTextInput(pti -> pti.actionId("usrname-request-action").multiline(false)))
                .label(plainText(pt -> pt.text("Username").emoji(true)))
                .optional(true)
                .dispatchAction(true)
        ));
        message.add( input(input -> input
                .blockId("psswrd-request-block")
                .element(plainTextInput(pti -> pti.actionId("psswrd-request-action").multiline(false)))
                .label(plainText(pt -> pt.text("Passwrd(Your passwrd is secure and will be used to trigger Edge job)").emoji(true)))
                .optional(true)
                .dispatchAction(true)
        ));
        message.add( actions(actions -> actions
                .elements(asElements(
                        button(b -> b.text(plainText(pt -> pt.emoji(true).text("Submit"))).value("submit").actionId("submitRequest")))
                )));
        return message;
    }

    public List<LayoutBlock>  buildRequestForDocker(String userId)
    {
        List<LayoutBlock> message = new ArrayList();
        message.add(SectionBlock
                .builder()
                .text(MarkdownTextObject
                        .builder()
                        .text("Hi <@" + userId + "> " + "Please fill below details")
                        .build())
                .build());
        message.add( input(input -> input
                .blockId("server-request-block")
                .element(plainTextInput(pti -> pti.actionId("server-request-action").multiline(false)))
                .label(plainText(pt -> pt.text("*Please provide Server IP* ").emoji(true)))
                .optional(true)
                .dispatchAction(true)
        ));
        message.add( actions(actions -> actions
                .elements(asElements(
                        button(b -> b.text(plainText(pt -> pt.emoji(true).text("Submit"))).value("submit").actionId("submitRequest")))
                )));
        return message;
    }


}
