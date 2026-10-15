const TopChannel = ({
  product,
}: {
  product: { id: number; name: string; sub: string };
}) => {
  return (
    <tr className="border-b">
      <td className="py-3 px-4 font-medium text-sm text-gray-700 dark:text-gray-300">
        {product.id}
      </td>
      <td className="py-3 px-4 whitespace-nowrap text-center text-sm text-gray-800 dark:text-gray-300">
        {product.name}
      </td>
      <td className="py-4 px-4 text-right text-sm text-gray-800 dark:text-gray-300">
        {product.sub}
      </td>
    </tr>
  );
};

const topChannels = [
  { id: 1, name: "Channel A", sub: "1M2" },
  { id: 2, name: "Channel B", sub: "500K" },
  { id: 3, name: "Channel C", sub: "350K" },
  { id: 4, name: "Channel D", sub: "120K" },
  { id: 5, name: "Channel D", sub: "256K" },
];

const TopChannels = () => {
  return (
    <div className="bg-[#D0D4B8] dark:bg-[#746c6b] dark:text-white rounded-xl p-6 shadow-sm pb-9">
      <h4 className="text-xl font-semibold text-gray-900 dark:text-white mb-4">
        Top Channels
      </h4>

      <div className="overflow-auto">
        <table className="min-w-full text-left text-sm">
          <thead className="bg-[#EAE7D6] rounded-xl shadow-sm dark:bg-[#c49a68]">
            <tr>
              <th className="py-3 px-4 rounded-l-lg font-medium text-gray-600 dark:text-white">
                #
              </th>
              <th className="py-3 px-4 font-medium text-gray-600 dark:text-white text-center">
                Name
              </th>
              <th className="py-3 px-4 rounded-r-lg font-medium text-gray-600 dark:text-white text-right">
                Subscribers
              </th>
            </tr>
          </thead>
          <tbody className=" dark:bg-[#746c6b]">
            {topChannels.map((product) => (
              <TopChannel key={product.id} product={product} />
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default TopChannels;
