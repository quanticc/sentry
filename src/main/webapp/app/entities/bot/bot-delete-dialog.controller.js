(function() {
    'use strict';

    angular
        .module('sentryApp')
        .controller('BotDeleteController',BotDeleteController);

    BotDeleteController.$inject = ['$uibModalInstance', 'entity', 'Bot'];

    function BotDeleteController($uibModalInstance, entity, Bot) {
        var vm = this;

        vm.bot = entity;
        vm.clear = clear;
        vm.confirmDelete = confirmDelete;

        function clear () {
            $uibModalInstance.dismiss('cancel');
        }

        function confirmDelete (id) {
            Bot.delete({id: id},
                function () {
                    $uibModalInstance.close(true);
                });
        }
    }
})();
