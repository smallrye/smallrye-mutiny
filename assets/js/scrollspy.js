$(document).ready(function() {
    var spy = new Gumshoe('#toc li a', {
        nested: false,

        navClass: 'x', // applied to the nav list item

        offset: "200",
        // reflow: true
    });
    spy.setup();
    spy.detect();

    $("#toc li")
        .click(function(e) {
            $("#toc li").removeClass("active");
            $(e.target).addClass("active");
            $(e.target).parent().addClass("active");
        });

    document.addEventListener('gumshoeActivate', function(event) {
        var li = event.target;
        $("#toc li").removeClass("active");
        $(li).addClass("active");
    }, false);

});