/*
 * Copyright 2005 Robert Cooper, Temple of the Screaming Penguin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.

 */
package com.rometools.modules.itunes.io;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rometools.modules.itunes.AbstractITunesObject;
import com.rometools.modules.itunes.EntryInformation;
import com.rometools.modules.itunes.EntryInformationImpl;
import com.rometools.modules.itunes.FeedInformationImpl;
import com.rometools.modules.itunes.ITunes;
import com.rometools.modules.itunes.types.Category;
import com.rometools.modules.itunes.types.Duration;
import com.rometools.modules.itunes.types.Subcategory;
import com.rometools.rome.io.ModuleParser;
import com.rometools.rome.io.WireFeedParser;

public class ITunesParser implements ModuleParser {

    private static final Logger LOG = LoggerFactory.getLogger(ITunesParser.class);

    Namespace ns = Namespace.getNamespace(AbstractITunesObject.URI);

    public ITunesParser() {
    }

    public void setParser(final WireFeedParser feedParser) {
    }

    @Override
    public String getNamespaceUri() {
        return AbstractITunesObject.URI;
    }

    @Override
    public com.rometools.rome.feed.module.Module parse(final Element element, final Locale locale) {
        AbstractITunesObject module = null;

        if (element.getName().equals("channel")) {
            final FeedInformationImpl feedInfo = new FeedInformationImpl();
            module = feedInfo;

            // Now I am going to get the channel specific tags
            final Element owner = element.getChild("owner", this.ns);

            if (owner != null) {
                final Element name = owner.getChild("name", this.ns);

                if (name != null) {
                    feedInfo.setOwnerName(name.getValue().trim());
                }

                final Element email = owner.getChild("email", this.ns);

                if (email != null) {
                    feedInfo.setOwnerEmailAddress(email.getValue().trim());
                }
            }

            final List<Element> categories = element.getChildren("category", this.ns);
            for (final Element element2 : categories) {
                final Element category = element2;
                if (category != null && category.getAttribute("text") != null) {
                    final Category cat = new Category();
                    cat.setName(category.getAttribute("text").getValue().trim());

                    final List<Element> subCategories = category.getChildren("category", this.ns);

                    for (final Element subCategory : subCategories) {
                        if (subCategory.getAttribute("text") != null) {
                            final Subcategory subcat = new Subcategory();
                            subcat.setName(subCategory.getAttribute("text").getValue().trim());
                            cat.addSubcategory(subcat);
                        }
                    }

                    feedInfo.getCategories().add(cat);
                }
            }

            final Element complete = element.getChild("complete", this.ns);
            if (complete != null) {
                feedInfo.setComplete("yes".equals(complete.getTextTrim().toLowerCase()));
            }

            final Element newFeedUrl = element.getChild("new-feed-url", this.ns);
            if (newFeedUrl != null) {
                try {
                    feedInfo.setNewFeedUrl(new URL(newFeedUrl.getTextTrim()));
                } catch (final MalformedURLException e) {
                    LOG.debug("Malformed URL Exception reading itunes:new-feed-url tag: {}", newFeedUrl.getTextTrim());
                }
            }

            final Element type = element.getChild("type", this.ns);
            if (type != null) {
                feedInfo.setType(type.getTextTrim());
            }

        } else if (element.getName().equals("item")) {
            final EntryInformationImpl entryInfo = new EntryInformationImpl();
            module = entryInfo;

            // Now I am going to get the item specific tags

            final Element duration = element.getChild("duration", this.ns);
            if (duration != null && duration.getValue() != null) {
                try {
                    final Duration dur = new Duration(duration.getValue().trim());
                    entryInfo.setDuration(dur);
                } catch (final Exception e) {
                    LOG.warn("Failed to parse duration: {}", duration.getValue());
                }
            }

            final Element isClosedCaptioned = element.getChild("isClosedCaptioned", this.ns);

            if (isClosedCaptioned != null && isClosedCaptioned.getValue() != null) {
                entryInfo.setClosedCaptioned(EntryInformation.ClosedCaptioned.valueOf(isClosedCaptioned.getTextTrim().toLowerCase()));
            }

            final Element order = element.getChild("order", this.ns);

            if (order != null && order.getValue() != null) {
                try {
                    entryInfo.setOrder(Integer.valueOf(order.getValue().trim()));
                } catch (final NumberFormatException e) {
                    LOG.warn("Failed to parse order: {}", order.getValue());
                }
            }

            final Element season = element.getChild("season", this.ns);

            if (season != null && season.getValue() != null) {
                try {
                    entryInfo.setSeason(Integer.valueOf(season.getValue().trim()));
                } catch (final NumberFormatException e) {
                    LOG.warn("Failed to parse season: {}", season.getValue());
                }
            }

            final Element episode = element.getChild("episode", this.ns);

            if (episode != null && episode.getValue() != null) {
                try {
                    entryInfo.setEpisode(Integer.valueOf(episode.getValue().trim()));
                } catch (final NumberFormatException e) {
                    LOG.warn("Failed to parse episode: {}", episode.getValue());
                }
            }

            final Element episodeType = element.getChild("episodeType", this.ns);

            if (episodeType != null && episodeType.getValue() != null) {
                entryInfo.setEpisodeType(episodeType.getTextTrim());
            }

            final Element title = element.getChild("title", this.ns);

            if (title != null && title.getValue() != null) {
                entryInfo.setTitle(title.getValue().trim());
            }

        }
        if (module != null) {
            // All these are common to both Channel and Item
            final Element author = element.getChild("author", this.ns);

            if (author != null && author.getText() != null) {
                module.setAuthor(author.getText());
            }

            final Element block = element.getChild("block", this.ns);

            // Ignore case of the value, assuming that any kind of "yes" clearly shows the intent.
            if (block != null && block.getValue() != null && block.getValue().trim().equalsIgnoreCase("Yes")) {
                module.setBlock(true);
            }

            final Element explicit = element.getChild("explicit", this.ns);

            if (explicit != null && explicit.getValue() != null) {
                module.setExplicit(ITunes.Explicit.valueOf(explicit.getTextTrim().toLowerCase()));
            }

            final Element keywords = element.getChild("keywords", this.ns);

            if (keywords != null) {
                final StringTokenizer tok = new StringTokenizer(this.getXmlInnerText(keywords).trim(), ",");
                final String[] keywordsArray = new String[tok.countTokens()];

                for (int i = 0; tok.hasMoreTokens(); i++) {
                    keywordsArray[i] = tok.nextToken();
                }

                module.setKeywords(keywordsArray);
            }

            final Element subtitle = element.getChild("subtitle", this.ns);

            if (subtitle != null) {
                module.setSubtitle(subtitle.getTextTrim());
            }

            final Element summary = element.getChild("summary", this.ns);

            if (summary != null) {
                module.setSummary(summary.getTextTrim());
            }

            final Element image = element.getChild("image", this.ns);

            if (image != null && image.getAttributeValue("href") != null) {
                try {
                    final URL imageURL = new URL(image.getAttributeValue("href").trim());
                    module.setImage(imageURL);
                } catch (final MalformedURLException e) {
                    LOG.warn("Malformed URL Exception reading itunes:image tag: {}", image.getAttributeValue("href"));
                }
            }

        }

        return module;
    }

    protected String getXmlInnerText(final Element e) {
        final StringBuffer sb = new StringBuffer();
        final XMLOutputter xo = new XMLOutputter();
        final List<Content> children = e.getContent();
        sb.append(xo.outputString(children));
        return sb.toString();
    }
}
