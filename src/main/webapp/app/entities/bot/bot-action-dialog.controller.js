(function() {
    'use strict';

    angular
        .module('sentryApp')
        .controller('BotActionController',BotActionController);

    BotActionController.$inject = ['$uibModalInstance', 'entity', 'Bot', 'action'];

    function BotActionController($uibModalInstance, entity, Bot, action) {
        var vm = this;

        vm.bot = entity;
        vm.action = action;
        vm.clear = clear;
        vm.confirmAction = confirmAction;

        function clear () {
            $uibModalInstance.dismiss('cancel');
        }

        function confirmAction (id, action) {
            Bot.action({id: id, action: action}, {},
                function () {
                    $uibModalInstance.close(true);
                });
        }
    }
})();
