/**
 * Select all options in a <select multiple>.
 * @param {HTMLSelectElement} selectBox - The target <select>.
 */
const selectAll = (selectBox) => {
  for (const opt of selectBox.options) {
    opt.selected = true;
  }
  $(selectBox).trigger('change');
};

/**
 * Deselect all options in a <select multiple>.
 * @param {HTMLSelectElement} selectBox - The target <select>.
 */
const selectNone = (selectBox) => {
  for (const opt of selectBox.options) {
    opt.selected = false;
  }
  $(selectBox).trigger('change');
};

$(() => {
  /** @type {JQuery} */
  const $menuItems = $('#dm-menu a');

  /**
   * Handle menu click: show linked panel and highlight the clicked item.
   * Expects each <a> to have a `name` attribute matching a panel's id.
   * @param {JQuery.ClickEvent} e
   */
  const onMenuClick = (e) => {
    e.preventDefault();

    const attr = $(e.currentTarget).attr('name');
    const $attr = $(`#${attr}`);

    $attr.siblings().hide();
    $attr.show();

    $menuItems.removeClass('highlight');
    $link.addClass('highlight');
  };

  $menuItems.on('click', onMenuClick);
});
