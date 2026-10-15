export const StatusSection: React.FC<{
  title: string;
  selectedStatuses: string[];
  onUpdate: (statuses: string[]) => void;
}> = ({ title, selectedStatuses, onUpdate }) => {
  const statuses = ["Active", "Banned", "Strike", "Warning"];

  const handleStatusChange = (status: string) => {
    const newStatuses = selectedStatuses.includes(status)
      ? selectedStatuses.filter((s) => s !== status)
      : [...selectedStatuses, status];
    onUpdate(newStatuses);
  };

  return (
    <div className="mb-4">
      <h3 className="text-lg font-semibold text-gray-700 mb-2">{title}</h3>
      <div className="flex flex-wrap gap-2">
        {statuses.map((status) => (
          <label
            key={status}
            className="flex items-center space-x-2"
          >
            <input
              type="checkbox"
              checked={selectedStatuses.includes(status)}
              onChange={() => handleStatusChange(status)}
              className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
            />
            <span className="text-sm text-gray-700">{status}</span>
          </label>
        ))}
      </div>
    </div>
  );
};
