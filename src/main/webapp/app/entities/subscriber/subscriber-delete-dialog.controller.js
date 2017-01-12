(function() {
    'use strict';

    angular
        .module('sentryApp')
        .controller('SubscriberDeleteController',SubscriberDeleteController);

    SubscriberDeleteController.$inject = ['$uibModalInstance', 'entity', 'Subscriber'];

    function SubscriberDeleteController($uibModalInstance, entity, Subscriber) {
        var vm = this;

        vm.subscriber = entity;
        vm.clear = clear;
        vm.confirmDelete = confirmDelete;

        function clear () {
            $uibModalInstance.dismiss('cancel');
        }

        function confirmDelete (id) {
            Subscriber.delete({id: id},
                function () {
                    $uibModalInstance.close(true);
                });
        }
    }
})();
