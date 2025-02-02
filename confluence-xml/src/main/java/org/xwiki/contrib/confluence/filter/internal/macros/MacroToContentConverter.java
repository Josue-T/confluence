/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.confluence.filter.internal.macros;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.confluence.filter.input.ConfluenceInputContext;
import org.xwiki.contrib.confluence.filter.input.ConfluenceInputProperties;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.syntax.Syntax;

/**
 * Converts a macro call to a group, retaining the id, class and content.
 *
 * @version $Id$
 * @since 9.51.1
 */
@Component(hints = { "ul", "legend", "auihorizontalnav", "auibuttongroup", "auihorizontalnavpage", "tableenhancer",
    "footnote" })
@Singleton
public class MacroToContentConverter extends AbstractMacroConverter
{
    private static final String DIV_CLASS_FORMAT = "confluence_%s_content";

    private static final String HTML_ATTRIBUTE_ID = "id";

    private static final String HTML_ATTRIBUTE_CLASS = "class";

    @Inject
    private ConfluenceInputContext context;

    @Inject
    private ComponentManager componentManager;

    @Override
    public void toXWiki(String id, Map<String, String> parameters, String content, boolean inline, Listener listener)
    {
        Map<String, String> divWrapperParams = new HashMap<>();

        List<String> classes = new ArrayList<>();
        classes.add(String.format(DIV_CLASS_FORMAT, id));
        if (parameters.containsKey(HTML_ATTRIBUTE_CLASS)) {
            classes.add(parameters.get(HTML_ATTRIBUTE_CLASS));
        }
        divWrapperParams.put(HTML_ATTRIBUTE_CLASS, String.join(" ", classes));

        if (parameters.containsKey(HTML_ATTRIBUTE_ID)) {
            divWrapperParams.put(HTML_ATTRIBUTE_ID, parameters.get(HTML_ATTRIBUTE_ID));
        }

        listener.beginGroup(divWrapperParams);
        ConfluenceInputProperties inputProperties = context.getProperties();
        Syntax macroContentSyntax = inputProperties == null ? null : inputProperties.getMacroContentSyntax();
        String syntaxId = macroContentSyntax != null ? macroContentSyntax.toIdString() : Syntax.XWIKI_2_1.toIdString();
        String newContent = toXWikiContent(id, parameters, content);
        try {
            Parser parser = componentManager.getInstance(Parser.class, syntaxId);
            XDOM contentXDOM = parser.parse(new StringReader(newContent));
            contentXDOM.getChildren().forEach(child -> child.traverse(listener));
        } catch (ComponentLookupException | ParseException e) {
            new MacroBlock("error", Collections.emptyMap(),
                String.format("Failed to parse the content of the [%s] macro with the syntax [%s].", id, syntaxId),
                false).traverse(listener);
        }
        listener.endGroup(divWrapperParams);
    }
}
