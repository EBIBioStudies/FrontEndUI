/*
 * Copyright 2009-2016 European Molecular Biology Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
var filesTable = null;
var linksTable = null;
var sectionTables = [];
var selectedFilesCount = 0;
var params = {};
var totalRows;


(function($, undefined) {
    if($ == undefined)
        throw "jQuery not loaded";

    $(function() {

        initialise();
        handleThumbnails();
        createDataTables();
        handleFileDownloadSelection();
        handleSectionArtifacts();

        // draw the main file table
        redrawTables();
        redrawTables(); // needed to adjust the column width. TODO: Find a better solution
        updateSelectedFiles(0);

        drawSubsections();
        handleAnchors();
        formatPageHtml();
        handleOrganisations();
        handleRightColumnClick();
        handleFileAttributeTableExpansion();
        handleSubattributes();
        handleOntologyLinks();
        handleImageURLs();
    });
})(window.jQuery);


function handleThumbnails() {
// capture hover before datatable is rendered
    $(".file-link").hover(
        function () {
            showThumbnail($(this));
        },
        function () {
            hideThumbnail($(this));
        }
    );
    $(".file-link").prev().hover(
        function () {
            showThumbnail($(this).next());
        },
        function () {
            hideThumbnail($(this).next());
        }
    );
}

function createDataTables() {
// create all sub-section file tables and hide them
    $(".file-list:not(#file-list)").each(function () {
        sectionTables.push($(this).DataTable({
            "dom": "t",
            paging: false
        }));
    });
    $(".ae-section-file").show();
    $(".ae-section-files").hide();
    $(".link-list:not(.link-widget)").each(function () {
        sectionTables.push($(this).DataTable({
            "dom": "t",
            paging: false
        }));
    })
    $(".ae-section-link").show();
    $(".ae-section-links").hide();
    $(".section-table").each(function () {
        sectionTables.push($(this).DataTable({
            "dom": "t",
            paging: false
        }));
    });
    $(".ae-section-table").show();
    $(".ae-section-tables").hide();
}

function handleFileDownloadSelection() {
    $("#file-list tbody").on('click', 'input[type="checkbox"]', function () {
        $(this).toggleClass('selected');
        updateSelectedFiles($(this).hasClass('selected') ? 1 : -1);
    });

    $("#file-list tbody tr td a").on('click', function () {
        event.stopPropagation();
    });

    $("#download-selected-files").on('click', function () {
        // select all checked input boxes and get the href in the links contained in their siblings
        var files = $.map($('input.selected', filesTable.column(0).nodes()), function (v) {
            return $(v).data('name');
        });
        downloadFiles(files);
    });

    $("#select-all-files").on('click', function () {
        var isChecked = $(this).is(':checked');
        if (!isChecked) {
            $('input[type="checkbox"]', filesTable.column(0).nodes()).removeAttr('checked');
            $('input[type="checkbox"]', filesTable.column(0).nodes()).removeClass('selected');
            selectedFilesCount = 0;
        } else {
            $('input[type="checkbox"]', filesTable.column(0).nodes()).attr('checked', 'checked');
            $('input[type="checkbox"]', filesTable.column(0).nodes()).addClass('selected');
            selectedFilesCount = totalRows;
        }
        updateSelectedFiles(0);
    });
}

function handleSectionArtifacts() {
    $(".toggle-files, .toggle-links, .toggle-tables").on('click', function () {
        var type = $(this).hasClass("toggle-files") ? "file" : $(this).hasClass("toggle-links") ? "link" : "table";
        var section = $(this).siblings('.ae-section-' + type + 's');
        if (section.css('display') == 'none') {
            section.show();
            redrawTables(true);
            $(this).html('<i class="fa fa-caret-down"></i> hide ' + type + ($(this).data('total') == '1' ? '' : 's'))
        } else {
            section.hide();
            $(this).html('<i class="fa fa-caret-right"></i> show ' + type + ($(this).data('total') == '1' ? '' : 's'))
        }
    });
    $(".toggle-files, .toggle-links, .toggle-tables").each(function () {
        var type = $(this).hasClass("toggle-files") ? "file" : $(this).hasClass("toggle-links") ? "link" : "table";
        $(this).html('<i class="fa fa-caret-right"></i> show ' + type + ($(this).data('total') == '1' ? '' : 's'));
    });

    //handle file attribute table icons
    $(".attributes-icon").on ('click', function () {
        closeFullScreen();
        var section = '#'+$(this).data('section-id');
        openHREF(section);
        var toggleLink = $(section).next().find('.toggle-tables').first();
        if (toggleLink.first().text().indexOf('show')>=0) toggleLink.click();

    });

    // add link type filters
    $(".link-filter").on('change', function() {
        var filters = $(".link-filter:checked").map(function() { return '^'+this.id+'$'}).get();
        if (filters.length==0) {
            filters = ['^$']
        }
        linksTable[$(this).data('position')-1].column(1).search(filters.join('|'),true, false).draw()
    });

}

function drawSubsections() {
// draw subsection and hide them
    $(".indented-section").parent().prev().prepend('<span class="toggle-section fa-fw fa fa-caret-right fa-icon" title="Click to expand"/>')
    $(".indented-section").hide();

    $('.toggle-section').parent().css('cursor', 'pointer');
    $('.toggle-section').parent().on('click', function () {
        var indented_section = $(this).next().children().first();
        if (indented_section.css('display') == 'none') {
            $(this).children().first().toggleClass('fa-caret-down').toggleClass('fa-caret-right').attr('title', 'Click to collapse');
            indented_section.show();
            redrawTables(true);
        } else {
            $(this).children().first().toggleClass('fa-caret-down').toggleClass('fa-caret-right').attr('title', 'Click to expand');
            indented_section.hide();
            redrawTables(true);
        }
    })
    // limit section title clicks
    $(".section-title-bar").click(function(e) {
        e.stopPropagation();
    })
}

function formatPageHtml() {
    //replace all newlines with html tags
    $('#ae-detail > .value').each(function () {
        var html = $(this).html();
        if (html.indexOf('<') < 0) { // replace only if no tags are inside
            $(this).html($(this).html().replace(/\n/g, '<br/>'))
        }
    });


    //handle escape key on fullscreen
    $(document).on('keydown',function ( e ) {
        if ( e.keyCode === 27 ) {
            closeFullScreen();
        }
    });
}

function handleOrganisations() {
    $('.org-link').click(function () {
        var href = $(this).attr('href');
        if (!$(href).is(':visible')) {
            $('#hidden-orgs').find('a.show-more').click()
        }

        $('html, body').animate({
            scrollTop: $(href).offset().top - 10
        }, 200);

        $(href).next().next().animate({opacity: 0.8}, 200, function () {
            $(href).next().next().css('background-color', 'yellow');
            $(href).next().next().animate({opacity: 0.4}, 400, function () {
                $(href).next().next().css('background-color', 'lightgray');
                $(href).next().next().animate({opacity: 1}, 600);
                $(href).next().next().css('background-color', 'transparent');
            })
        });

    });
}

function handleRightColumnClick() {
// right column expansion
    $('#right-column-expander').click(function () {
        $(this).toggleClass('fa-close').toggleClass('fa-expand');
        $(this).attr('title', $(this).hasClass('fa-expand') ? 'Click to expand' : 'Click to close');
        $('html').toggleClass('stop-scrolling');
        $('#blocker').toggleClass('blocker');
        $('#ae-detail-right-column').toggleClass('fullscreen');
        $(".file-list-file-name > a:nth-child(1)").css("max-width", $(this).hasClass('fa-expand') ? '200px' : '500px');
        $("table.link-widget tbody td a").css("max-width", $(this).hasClass('fa-expand') ? '200px' : '500px');
        $('#right-column-wrapper').css('max-height', $('#ae-detail-right-column').hasClass('fullscreen')
            ? (parseInt($(window).height()) * 0.80) + 'px' : 'auto');
        redrawTables();
    });
}

function handleFileAttributeTableExpansion() {
// file attribute table expansion
    $('.table-expander').click(function () {
        $(this).toggleClass('fa-close').toggleClass('fa-expand');
        $(this).attr('title', $(this).hasClass('fa-expand') ? 'Click to expand' : 'Click to close');
        $('html').toggleClass('stop-scrolling');
        $('#blocker').toggleClass('blocker');
        $(this).parent().parent().toggleClass('fullscreen');
        $("table.dataTable tbody td a").css("max-width", $(this).hasClass('fa-expand') ? '200px' : '500px');
        $('.table-wrapper').css('height', 'auto');
        $('.fullscreen .table-wrapper').css('max-height', (parseInt($(window).height()) * 0.80) + 'px').css('top', '45%');
    });
}

function handleSubattributes() {
// handle sub-attributes (shown with an (i) sign)
    $('.sub-attribute-info').hover(
        function () {
            $(this).next().css('display', 'inline-block');
            $(this).prev().toggleClass('sub-attribute-text');
        }, function () {
            $(this).next().css('display', 'none');
            $(this).prev().toggleClass('sub-attribute-text');
        }
    );
}

function handleOntologyLinks() {
// handle ontology links
    $("span[data-term-id][data-ontology]").each(function () {
        var ont = $(this).data('ontology').toLowerCase();
        var termId = $(this).data('term-id');
        var name = $(this).data('term-name');
        $.ajax({
            async: true,
            context: this,
            url: "http://www.ebi.ac.uk/ols/beta/api/ontologies/" + ont + "/terms",
            data: {short_form: termId, size: 1},
            success: function (data) {
                if (data && data._embedded && data._embedded.terms && data._embedded.terms.length > 0) {
                    var n = name ? name : data._embedded.terms[0].description ? data._embedded.terms[0].description : null;
                    $(this).append('<a title="' + data._embedded.terms[0].obo_id +
                        ( n ? ' - ' + n : '') + '" ' +
                        'class="ontology-icon"  target="_blank" href="' + data._embedded.terms[0].iri
                        + '"><span class="icon icon-conceptual" data-icon="o"></span></a>');
                }
            }
        });

    });
}

function handleImageURLs() {
// handle image URLs
    $(".sub-attribute:contains('Type:Image URL')").each(function () {
        var url = $(this).parent().clone().children().remove().end().text();
        $(this).parent().html('<img class="url-image" src="' + url + '"/>');
    });
}

//initialise variables and page controls
function initialise() {
    totalRows = $("#file-list tbody tr").length;

    // parse params
    $(location.search.substr(1).split('&')).each(function(i,v) {
        var kv = v.split('=');
        var k = kv[0], v = kv[1] && decodeURIComponent(kv[1]).replace(/\+/g, ' ');
        (k in params) ? params[k]=params[k]+'|'+v : params[k] = v;
    });

    // add modal blocker div
    $('body').append('<div id="blocker"/><div id="tooltip"/>');

    //turn off all selected files
    $('input:checkbox:not(.do-not-clear)').prop('checked', false);

    $(window).resize(function () {
        redrawTables();
    });

}

function closeFullScreen() {
    $('.table-expander','.fullscreen').click();
    $('#right-column-expander','.fullscreen').click();
}

//handle clearing search filter
function clearFilter() {
    filesTable.search('').columns().search('').draw();
}

function openHREF(href) {
    var section = $(href);
    var o = section;
    while (o.prop("tagName")!=='BODY') {
        var p =  o.parent().parent();
        if(o.parent().css('display')!='block') {
            p.prev().click();
        }
        o = p;
    }
    if(section.next().children().first().css('display')=='none') {
        section.click();
    }

    $('html, body').animate({
        scrollTop: $(section).offset().top -10
    }, 200);
}

function handleAnchors() {
    // handle clicks on section links in main file table
    $("a[href^='#']", "#file-list" ).filter(function(){ return $(this).attr('href').length>1 }).click( function(){
        var subsec = $(this).attr('href');
        closeFullScreen();
        openHREF(subsec);
    });

    // handle clicks on file filters in section
    $("a[data-files-id]").click( function() {
        $('#right-column-expander').click();
        filesTable.column(2).search('^'+$(this).data('files-id')+'$',true,false).draw();
    });

    // scroll to main anchor
    if (location.hash) {
        openHREF(location.hash);
    }

    // add file search filter
    if (params['fs']) {
        $('#right-column-expander').click();
        filesTable.search(params['fs']).draw();
    }



}

function showThumbnail(fileLink) {
    $(fileLink).data('isFocused', true);
    if (!$(fileLink).data("loaded")) {
        $(fileLink).addClass("fa-spin");
        $("img",$(fileLink)).attr("src",($(fileLink).data("thumbnail")));
    } else {
        $("#tooltip").html($("img",$(fileLink)).clone());
        var o = $(".thumbnail-image", $(fileLink)).offset()
        $("#tooltip").css("top",o.top).css("left",o.left);
        $("#tooltip").fadeIn();
    }
    $("img", $(fileLink)).one("load",function() {
        $(fileLink).data("loaded",true);
        $(fileLink).removeClass("fa-spin");
        if ($(fileLink).data('isFocused')) { // focus is still on the link
            $("#tooltip").html($(this).clone());
            var o = $(".thumbnail-image", $(this).parent()).offset();
            $("#tooltip").css("top", o.top).css("left", o.left);
            $("#tooltip").fadeIn();
        }
    });
}

function hideThumbnail(fileLink) {
    $(fileLink).removeClass('fa-spin');
    $('#tooltip').stop().hide();
    $(fileLink).data('isFocused', false);
}



function downloadFiles(files) {
    var html = '';
    if (files.length==1) {
        html += '<form method="GET" target="_blank" action="'
            + contextPath + "/files/"
            + $('.accessionNumber').text() + '/' + files[0]+'" />';
    } else {
        html += '<form method="POST" target="_blank" action="'
            + contextPath + "/files/"
            + $('.accessionNumber').text() + '/zip'+location.search+'">';
        $(files).each( function(i,v) {
            html += '<input type="hidden" name="files" value="'+v+'"/>'
        });
        html += '</form>';
    }
    var submissionForm = $(html);
    $('body').append(submissionForm);
    $(submissionForm).submit();
}

function updateSelectedFiles(inc)
{

    if (!filesTable || !filesTable.rows() || !filesTable.rows().eq(0) ) return;
    selectedFilesCount += inc;
    $("#selected-file-text").text( (selectedFilesCount == 0
            ? 'No ' : selectedFilesCount)
        +' file'+(selectedFilesCount>1 ? 's':'')+' selected');
    if (selectedFilesCount==0) {
        $('#download-selected-files').hide();
    } else {
        $('#download-selected-files').show();
        $('#download-selected-files').text('Download' + (selectedFilesCount==2
                ? ' both'
                : selectedFilesCount>1 ? ' all '+selectedFilesCount : ''));
    }

    if (selectedFilesCount==totalRows)
        $("#select-all-files").attr('checked', 'checked');
    else
        $("#select-all-files").removeAttr('checked');

}

function redrawTables(drawSectionTablesOnly) {

    if (!drawSectionTablesOnly) {
        if (filesTable == null) {
            filesTable = $("#file-list").DataTable({
                "lengthMenu": [[5, 10, 25, 50, 100], [5, 10, 25, 50, 100]],
                "columnDefs": [ {"targets": [0], "searchable": false, "orderable": false, "visible": true},
                    {"targets": [2], "searchable": true, "orderable": false, "visible": false}],
                "order": [[1, "asc"]],
                "dom": "rlftpi",
                "scrollX": "100%",
                "infoCallback": function( settings, start, end, max, total, out ) {
                    return (total== max) ? out : out +'<a class="section-button" id="clear-filter" onclick="clearFilter();return false;">' +
                    '<span class="fa-stack bs-icon-fa-stack">' +
                        '<i class="fa fa-filter fa-stack-1x"></i>' +
                        '<i class="fa-stack-1x filter-cross">×</i>' +
                    '</span> clear filter</a>';
                }
            });
        } else {
            filesTable.columns.adjust().draw();
        }

        if (linksTable == null) {
            linksTable = [];
            $(".link-widget").each(function () {
                linksTable.push($(this).DataTable({
                        "lengthMenu": [[5, 10, 25, 50, 100], [5, 10, 25, 50, 100]],
                        "dom": "rlftpi",
                        "scrollX": "100%"
                    })
                );
            });
        } else {
            $(linksTable).each(function () {
                this.columns.adjust().draw();
                window
            });
        }

        $(".list-content").show()
        $(".list-loader").hide();
    }
    $(sectionTables).each(function() {
        this.columns.adjust().draw();
    });

}