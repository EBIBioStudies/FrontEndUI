/**
 * Created by ehsan on 22/11/2016.
 */

    $(function() {
        $( document ).ready(function() {
            var url = decodeURIComponent(window.location.href);
            $('input[type=checkbox].facet-value').each(function () {
                if(url.indexOf(this.value+",")>0)
                    this.checked = true;
            });
        });

        $('#browsehecatos').click(function(event) {
            var sList = "";
            $('input[type=checkbox].facet-value').each(function () {
                var sThisVal = (this.checked ? this.value : "");
                if(sThisVal!="")
                    sList += (sList=="" ? sThisVal : "," + sThisVal);
            });
            var loc = window.location.href;
            if(loc.indexOf("?facets")>0)
                loc = loc.replace(/\?facets=.*/g, "");
            else if(loc.indexOf("&facets")>0)
                    loc = loc.replace(/\&facets=.*/g, "");

            sList = encodeURIComponent(sList+",");
            if(loc.indexOf("?")>=0)
                sList = "&facets="+sList;
            else
                sList = "?facets="+sList;
            window.location = loc+sList;
        });
    });
