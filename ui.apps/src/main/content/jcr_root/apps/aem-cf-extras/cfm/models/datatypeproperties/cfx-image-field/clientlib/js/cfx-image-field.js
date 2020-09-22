(function($) {
    const IMAGEFIELD_SELECTOR = ".htl-imagefield";
    const IMAGEFIELD_AUTOCOMPLETE_SELECTOR  = IMAGEFIELD_SELECTOR + " foundation-autocomplete";
    const IMAGE_EXTENSIONS = [".png", ".jpg", ".jpeg", ".svg"];
    let registry = $(window).adaptTo("foundation-registry");

    registry.register("foundation.validation.selector", {
        submittable: IMAGEFIELD_AUTOCOMPLETE_SELECTOR,
        candidate: IMAGEFIELD_AUTOCOMPLETE_SELECTOR,
        exclusion: IMAGEFIELD_AUTOCOMPLETE_SELECTOR + " *"
    });

    registry.register("foundation.validation.validator", {
        selector: IMAGEFIELD_AUTOCOMPLETE_SELECTOR,
        validate: function(el) {
            updateThumbnail(el);

            if (el.required) {
                if (el.multiple && el.values.length === 0) {
                    return Granite.I18n.get("Please fill out this field.");
                } else if (!el.multiple && el.value.length === 0) {
                    return Granite.I18n.get("Please fill out this field.");
                }
            }

            if (!checkExtension(el.value)) {
                return Granite.I18n.get("Please select a valid image file.")
            }
        }
    });

    // Not all change events seem to trigger the granite validator so this is here for extra coverage
    $(IMAGEFIELD_AUTOCOMPLETE_SELECTOR).change(function() {
        $(this).checkValidity();
    });

    function updateThumbnail(imageField) {
        let imagePath = imageField.value;
        if (imagePath === undefined) {
            return;
        }
        let $parentHtlImageField = $(imageField).parents(IMAGEFIELD_SELECTOR);
        let $imageElement = $parentHtlImageField.find('img');

        if (imagePath) {
            $imageElement.attr('src', imagePath.replace(/&amp;/g,"&"));
        } else {
            $imageElement.removeAttr('src');
        }
    }

    function checkExtension(imagePath) {
        if (!imagePath) {
            return true;
        }

        for (let i = 0; i < IMAGE_EXTENSIONS.length; i++) {
            if (imagePath.toLowerCase().endsWith(IMAGE_EXTENSIONS[i])) {
                return true;
            }
        }

        return false;
    }

} (jQuery));